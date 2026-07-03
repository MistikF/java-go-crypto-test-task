package com.example.cryptojava.web;

import com.example.cryptojava.crypto.Pkcs7Service;
import com.example.cryptojava.persistence.OperationLogger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Base64;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SignatureController.class)
class SignatureControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private Pkcs7Service pkcs7Service;

    @MockBean
    private OperationLogger operationLogger;

    @Test
    void signReturnsBase64Signature() throws Exception {
        when(pkcs7Service.sign(any(), anyBoolean())).thenReturn(new byte[]{1, 2, 3});
        String payload = "{\"data\":\"" + Base64.getEncoder().encodeToString("hi".getBytes()) + "\",\"detached\":false}";

        mockMvc.perform(post("/api/v1/signatures/sign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.signature").value(Base64.getEncoder().encodeToString(new byte[]{1, 2, 3})));
    }

    @Test
    void signRejectsBlankData() throws Exception {
        mockMvc.perform(post("/api/v1/signatures/sign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"data\":\"\",\"detached\":false}"))
                .andExpect(status().isBadRequest());
    }
}
