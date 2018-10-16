package test.utils.json;

import com.firefly.utils.json.annotation.JsonProperty;

import java.util.Objects;

/**
 * @author Pengtao Qiu
 */
public class Common {

    @JsonProperty("client_id")
    private String clientId;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Common common = (Common) o;
        return Objects.equals(clientId, common.clientId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientId);
    }
}
