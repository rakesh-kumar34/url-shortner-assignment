package com.rakesh.urlshortener;

import com.rakesh.urlshortener.api.Errors;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;

class StorageFailureTest {
    @Test void connectionFailureReturnsCorrelatedRetryableErrorWithoutDetails() throws Exception {
        @RestController class UnavailableStorage {
            @GetMapping("/storage-probe") String probe() {
                throw new CannotCreateTransactionException("jdbc:private-host password=do-not-disclose");
            }
        }
        var mvc = MockMvcBuilders.standaloneSetup(new UnavailableStorage()).setControllerAdvice(new Errors()).build();
        mvc.perform(get("/storage-probe").requestAttr("requestId", "failure-probe"))
                .andExpect(status().isServiceUnavailable()).andExpect(header().string("Retry-After", "1"))
                .andExpect(jsonPath("$.code").value("storage_unavailable"))
                .andExpect(jsonPath("$.requestId").value("failure-probe"))
                .andExpect(content().string(not(containsString("private-host"))))
                .andExpect(content().string(not(containsString("do-not-disclose"))));
    }
}
