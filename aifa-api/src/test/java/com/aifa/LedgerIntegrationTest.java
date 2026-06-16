package com.aifa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class LedgerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerCreateWalletAddTransactionUpdatesBalance() throws Exception {
        String email = "ledger" + System.nanoTime() + "@aifa.test";

        MvcResult register = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"password123"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode auth = objectMapper.readTree(register.getResponse().getContentAsString());
        String accessToken = auth.get("accessToken").asText();

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

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "walletId":"%s",
                                  "amountRwf":50000,
                                  "type":"income",
                                  "description":"Test deposit",
                                  "transactionAt":"%s"
                                }
                                """.formatted(walletId, Instant.now().toString())))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/wallets").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].balanceRwf").value(50000));

        MvcResult dashboard = mockMvc.perform(get("/api/v1/dashboard/summary")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode summary = objectMapper.readTree(dashboard.getResponse().getContentAsString());
        assertThat(summary.get("totalBalanceRwf").asLong()).isEqualTo(50000L);
    }
}
