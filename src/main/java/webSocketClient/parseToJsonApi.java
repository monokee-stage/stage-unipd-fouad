package webSocketClient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.Response;
import org.apache.directory.api.ldap.model.message.SearchResultEntry;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class parseToJsonApi {
    JSONObject json=new JSONObject();

    public String getJson(){
        return json.toJSONString();
    }
    public void parseJson(SearchCursor search,String nameTypeSearch) throws CursorException, LdapException {
        Gson g=new GsonBuilder().disableHtmlEscaping().create();
        JSONArray array = new JSONArray();
        while ( search.next() )
        {
            Response response = search.get();
            // process the SearchResultEntry
            if ( response instanceof SearchResultEntry)
            {
                Entry resultEntry = ( ( SearchResultEntry ) response ).getEntry();
                accountOrGroupApi a =new accountOrGroupApi(resultEntry);
                array.add(g.toJson(a));
            }
        }
        json.put(nameTypeSearch,array);
    }
}
