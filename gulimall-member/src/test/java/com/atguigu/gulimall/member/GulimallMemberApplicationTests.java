package com.atguigu.gulimall.member;

import org.apache.commons.codec.digest.Md5Crypt;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

//@SpringBootTest
class GulimallMemberApplicationTests {

    @Test
    void contextLoads() {
        //$1$qznsswwp$8.PCPqPV9aN9BPQ8RrrVv.
        //$1$pUj7ZItq$sHNabjsfZ0JWtgk7h6.Cf.
        String code = Md5Crypt.md5Crypt("123456".getBytes());
        //$1$123456$wOSEtcyiP2N/IfIl15W6Z0
        String code_my = Md5Crypt.md5Crypt("123456".getBytes(), "$1$123456");
        String code_my_now = Md5Crypt.md5Crypt("123789".getBytes(), "$1$123456");

        System.out.println(code);
        System.out.println(code_my);
        System.out.println(code_my_now);
    }

}
