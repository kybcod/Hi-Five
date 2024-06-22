package com.backend.service.product;

import com.backend.domain.product.Product;
import com.backend.domain.product.ProductFile;
import com.backend.mapper.auction.AuctionMapper;
import com.backend.mapper.product.ProductMapper;
import com.backend.util.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class ProductService {

    private final ProductMapper mapper;
    private final AuctionMapper auctionMapper;
    private final S3Client s3Client;

    @Value("${aws.s3.bucket.name}")
    String bucketName;

    @Value("${image.src.prefix}")
    String srcPrefix;

    public void upload(Product product, MultipartFile[] files, Authentication authentication) throws IOException {
        Integer userId = Integer.valueOf(authentication.getName());
        product.setUserId(userId);

        mapper.insert(product);

        //파일 추가
        if (files != null) {
            for (MultipartFile file : files) {
                System.out.println("file.getOriginalFilename() = " + file.getOriginalFilename());
                // DB 저장
                mapper.insertFile(product.getId(), file.getOriginalFilename());

                //실제 파일 저장
                String key = STR."prj3/\{product.getId()}/\{file.getOriginalFilename()}";
                PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .acl(ObjectCannedACL.PUBLIC_READ)
                        .build();

                s3Client.putObject(putObjectRequest,
                        RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            }
        }
    }

    public List<Product> list() {
        List<Product> products = mapper.selectAll();
        settingFilePath(products);
        return products;
    }


    public Map<String, Object> getList(Pageable pageable, String keyword, String category) {
        List<Product> content = mapper.selectWithPageable(pageable, keyword, category);
        settingFilePath(content);

        int total = mapper.selectTotalCount(keyword, category);
        Page<Product> page = new PageImpl<>(content, pageable, total);
        PageInfo pageInfo = new PageInfo().setting(page);
        return Map.of("content", content, "pageInfo", pageInfo);
    }

    public Map<String, Object> get(Integer id, Authentication authentication) {
        Map<String, Object> result = new HashMap<>();

        mapper.updateViewCount(id);

        Product product = mapper.selectById(id);
        settingFilePath(Collections.singletonList(product));

        //좋아요
        Map<String, Object> like = new HashMap<>();
        if (authentication == null) {
            like.put("like", false);
        } else {
            int i = mapper.selectLikeByProductIdAndUserId(id, authentication.getName());
            like.put("like", i == 1);
        }
        like.put("count", mapper.selectCountLikeByProductId(id));

        result.put("product", product);
        result.put("like", like);
        result.put("productFileList", product.getProductFileList());
        return result;
    }

    public void edit(Product product, List<String> removedFileList, MultipartFile[] newFileList) throws IOException {
        if (removedFileList != null && removedFileList.size() > 0) {
            //s3 파일 삭제
            for (String name : removedFileList) {
                String key = STR."prj3/\{product.getId()}/\{name}";
                DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build();
                s3Client.deleteObject(deleteObjectRequest);

                mapper.deleteFileByProductIdAndFileName(product.getId(), name);
            }
        }

        if (newFileList != null && newFileList.length > 0) {
            List<ProductFile> productFileList = mapper.selectFileByProductId(product.getId());
            List<String> fileNameList = productFileList.stream().map(ProductFile::getFileName).toList();

            for (MultipartFile file : newFileList) {
                System.out.println("file.getOriginalFilename() = " + file.getOriginalFilename());
                String name = file.getOriginalFilename();
                if (!fileNameList.contains(name)) {
                    mapper.insertFile(product.getId(), name);
                }

                //실제 파일 저장
                String key = STR."prj3/\{product.getId()}/\{name}";
                PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .acl(ObjectCannedACL.PUBLIC_READ)
                        .build();

                s3Client.putObject(putObjectRequest,
                        RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            }
        }
        mapper.update(product);
    }

    public void remove(Integer id) {

        Product product = mapper.selectById(id);

        // chat 삭제
        // chat_room_id 가져오기
        List<Integer> chatRoomIds = mapper.selectChatByChatRoomId(id);
        for (Integer chatRoomId : chatRoomIds) {
            mapper.deleteChatByChatRoomId(chatRoomId);
        }
        // chat_room 삭제
        mapper.deleteChatRoomBySellerId(product.getUserId());
        mapper.deleteChatRoomByProductId(id);


        // s3에서 파일(이미지) 삭제
        List<ProductFile> productFileList = mapper.selectFileByProductId(id);
        for (ProductFile productFile : productFileList) {
            String key = STR."prj3/\{id}/\{productFile.getFileName()}";
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            s3Client.deleteObject(deleteObjectRequest);
        }

        // 입찰 내역 삭제
        mapper.deleteBidListByProductId(id);

        //좋아요 삭제
        mapper.deleteLikeByProductId(id);

        //파일 DB 삭제
        mapper.deleteFileByProductId(id);

        // 상품 삭제
        mapper.deleteByProductId(id);
    }


    public Map<String, Object> getProductsByUserId(Integer userId, Pageable pageable) {
        List<Product> productList = mapper.selectProductsByUserIdWithPagination(userId, pageable);
        String userNickName = mapper.selectUserNickName(userId);
        settingFilePath(productList);

        // 더보기 : 페이지
        int total = mapper.selectTotalCountByUserId(userId);
        Page<Product> page = new PageImpl<>(productList, pageable, total);
        PageInfo pageInfo = new PageInfo().setting(page);
        boolean hasNextPage = pageable.getPageNumber() + 1 < page.getTotalPages();


        return Map.of("productList", productList, "pageInfo", pageInfo, "hasNextPage", hasNextPage, "userNickName", userNickName);
    }

    public Map<String, Object> getProductsLikeByUserId(Integer userId, Pageable pageable) {
        // 좋아요
        List<Product> likeProductList = mapper.selectLikeSelectByUserId(userId, pageable);
        settingFilePath(likeProductList);

        int total = mapper.selectCountLikeByUserId(userId);

        // 더보기 : 페이지
        PageImpl<Product> page = new PageImpl<>(likeProductList, pageable, total);
        PageInfo pageInfo = new PageInfo().setting(page);
        boolean hasNextPage = pageable.getPageNumber() + 1 < page.getTotalPages();

        return Map.of("likeProductList", likeProductList, "pageInfo", pageInfo, "hasNextPage", hasNextPage);
    }

    // TODO : 코드 정리
//    public ProductListResponse getById(Integer id, Authentication authentication) {
//        mapper.updateViewCount(id);
//
//        ProductWithUserDTO product = mapper.selectById2(id);
//
//        List<String> productFiles = mapper.selectFileByProductId(product.getId());
//        List<ProductFile> files = productFiles.stream()
//                .map(fileName -> new ProductFile(fileName, STR."\{srcPrefix}\{product.getId()}/\{fileName}"))
//                .toList();
////        product.setProductFileList(files);
//
//        //좋아요
//        Like like = new Like();
//        if (authentication == null) {
//            like.setLike(false);
//        } else {
//            int i = mapper.selectLikeByProductIdAndUserId(id, authentication.getName());
//            like.setLike(i == 1);
//        }
//        like.setCount(mapper.selectCountLikeByProductId(id));
//
//        return new ProductListResponse(product, like, files);
//    }

    public List<Product> selectProductAndBidList() {
        return mapper.selectProductAndBidList();
    }

    public void updateStatus(Product product) {
        mapper.updateStatus(product);
    }

    // s3에 파일 조회
    public void settingFilePath(List<Product> products) {
        products.forEach(product -> product.getProductFileList().forEach(
                productFile -> productFile.setFilePath(srcPrefix)
        ));
    }


    public boolean validate(Product product) {
        if (product.getTitle() == null || product.getTitle().isBlank()) {
            return false;
        }
        if (product.getCategory() == null || product.getCategory().isBlank()) {
            return false;
        }
        if (product.getStartPrice() == null || product.getStartPrice().isBlank()) {
            return false;
        }
        if (product.getEndTime() == null) {
            return false;
        }
        return true;
    }

    //userId 받고 있는지 확인하기 위해 즉 로그인 했는지
    public boolean hasAccess(Integer id, Authentication authentication) {

        Product product = mapper.selectById(id);
        return product.getUserId().equals(Integer.valueOf(authentication.getName()));
    }
}



