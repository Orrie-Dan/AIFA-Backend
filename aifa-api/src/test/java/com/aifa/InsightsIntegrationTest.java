package com.aifa;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class InsightsIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void insightsEndpointsReturnDataForUserWithTransactions() throws Exception {
        String email = "insights" + System.nanoTime() + "@aifa.test";
        String accessToken = registerAndLogin(email);

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

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/v1/transactions")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "walletId":"%s",
                                      "amountRwf":200000,
                                      "type":"income",
                                      "merchantName":"Employer Ltd",
                                      "transactionAt":"%s"
                                    }
                                    """.formatted(walletId, Instant.now().minusSeconds(86400L * 30 * i).toString())))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/api/v1/transactions")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "walletId":"%s",
                                      "amountRwf":50000,
                                      "type":"expense",
                                      "merchantName":"Simba Supermarket",
                                      "transactionAt":"%s"
                                    }
                                    """.formatted(walletId, Instant.now().minusSeconds(86400L * 30 * i).toString())))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/api/v1/insights/spending-analysis")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ready"));

        mockMvc.perform(post("/api/v1/insights/affordability")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itemPriceRwf":100000,"targetDate":"%s"}
                                """.formatted(LocalDate.now().plusMonths(6))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confidence").exists());

        mockMvc.perform(get("/api/v1/insights/recommendations")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/dashboard/summary").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.healthScore").exists())
                .andExpect(jsonPath("$.recommendations").isArray());
    }

    private String registerAndLogin(String email) throws Exception {
        MvcResult register = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"password123"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode auth = objectMapper.readTree(register.getResponse().getContentAsString());
        return auth.get("accessToken").asText();
    }
}
