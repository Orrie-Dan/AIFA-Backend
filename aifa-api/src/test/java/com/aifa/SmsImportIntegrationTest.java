package com.aifa;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class SmsImportIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void previewPersistsOutDirection() throws Exception {
        String email = "sms-import" + System.nanoTime() + "@aifa.test";

        MvcResult register = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"password123"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();

        String accessToken = objectMapper
                .readTree(register.getResponse().getContentAsString())
                .get("accessToken")
                .asText();

        MvcResult walletResult = mockMvc.perform(post("/api/v1/wallets")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"MTN MoMo","type":"mobile_money","primary":true}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String walletId = objectMapper
                .readTree(walletResult.getResponse().getContentAsString())
                .get("id")
                .asText();

        mockMvc.perform(post("/api/v1/import/sms")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "walletId":"%s",
                                  "messages":"*165*S*1500 RWF transferred to Jack TURIKUMANA (250783955909) at 2026-06-01 07:16:12. Fee: 100 RWF. Your new balance is RWF 4383"
                                }
                                """.formatted(walletId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].direction").value("OUT"))
                .andExpect(jsonPath("$.rows[0].amountRwf").value(1600));
    }
}
