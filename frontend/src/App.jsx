import { ChakraProvider } from "@chakra-ui/react";
import { createBrowserRouter, RouterProvider } from "react-router-dom";
import { Home } from "./Home.jsx";
import { ProductList } from "./product/ProductList.jsx";
import { ProductUpload } from "./product/ProductUpload.jsx";
import { MainProduct } from "./product/MainProduct.jsx";
import { ProductEdit } from "./product/ProductEdit.jsx";
import { ProductView } from "./product/ProductView.jsx";
import { SignUp } from "./user/SignUp.jsx";
import { Login } from "./Login.jsx";
import { LoginProvider } from "./component/LoginProvider.jsx";
import axios from "axios";
import { QuestionWrite } from "./Question/QuestionWrite.jsx";
import { QuestionList } from "./Question/QuestionList.jsx";
import { BoardWrite } from "./board/BoardWrite.jsx";
import { BoardList } from "./board/BoardList.jsx";
import { QuestionView } from "./Question/QuestionView.jsx";
import { QuestionEdit } from "./Question/QuestionEdit.jsx";

axios.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

const router = createBrowserRouter([
  {
    path: "/",
    element: <Home />,
    children: [
      { index: true, element: <MainProduct /> },
      { path: "write", element: <ProductUpload /> },
      { path: "list", element: <ProductList /> },
      { path: "edit/:id", element: <ProductEdit /> },
      { path: "product/:id", element: <ProductView /> },
      { path: "signup", element: <SignUp /> },
      { path: "login", element: <Login /> },
      { path: "question/write", element: <QuestionWrite /> },
      { path: "question/list", element: <QuestionList /> },
      { path: "question/:id", element: <QuestionView /> },
      { path: "question/edit/:id", element: <QuestionEdit /> },
      { path: "board", element: <BoardWrite /> },
      { path: "board/list", element: <BoardList /> },
    ],
  },
]);

function App() {
  return (
    <LoginProvider>
      <ChakraProvider>
        <RouterProvider router={router} />
      </ChakraProvider>
    </LoginProvider>
  );
}

export default App;
