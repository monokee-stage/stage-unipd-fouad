package webSocketClient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Vector;

public class parseToJsonConnId {
    private JSONObject json=new JSONObject();

    public String getJson(){
        return json.toJSONString();
    }
    public void parseJson(Vector<ConnectorObject> search, String nameTypeSearch) {
        Gson g=new GsonBuilder().disableHtmlEscaping().create();
        JSONArray array = new JSONArray();
        for (ConnectorObject result : search) {
            accountOrGroupLdap accGroupLdap=new accountOrGroupLdap(result);
            array.add(g.toJson(accGroupLdap));
        }
        json.put(nameTypeSearch,array);
    }
}
