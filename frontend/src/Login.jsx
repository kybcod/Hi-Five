import { Box, Button, FormControl, FormLabel, Input } from "@chakra-ui/react";
import { useContext, useState } from "react";
import axios from "axios";
import { LoginContext } from "./component/LoginProvider.jsx";

export function Login() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const account = useContext(LoginContext);

  function handleLogin() {
    axios
      .post("api/users/login", { email, password })
      .then((res) => account.login("token", res.data.token));
  }

  return (
    <Box>
      <Box>로그인</Box>
      <FormControl>
        <FormLabel>이메일</FormLabel>
        <Input onChange={(e) => setEmail(e.target.value)} />
      </FormControl>
      <FormControl>
        <FormLabel>비밀번호</FormLabel>
        <Input
          type={"password"}
          onChange={(e) => setPassword(e.target.value)}
        />
      </FormControl>
      <Button onClick={handleLogin}>로그인</Button>
    </Box>
  );
}
