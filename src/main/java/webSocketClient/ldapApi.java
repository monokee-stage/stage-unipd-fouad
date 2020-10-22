package webSocketClient;


import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.*;
import org.apache.directory.api.ldap.model.message.controls.PagedResults;
import org.apache.directory.api.ldap.model.message.controls.PagedResultsImpl;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.util.Strings;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import java.util.*;

public class ldapApi {
    LdapConnection connection;
    SearchRequest reqGroups = null;
    SearchRequest reqAccounts =  null;
    final private String pathJson="src/main/java/webSocketClient/accountGroupConfigApi.json";
    socketClient socketClient;
    int page;
    private void setReqAccounts(JSONObject accounts){
        try {
            /********* parse accounts from json *******/
            JSONObject jsonAccounts= (JSONObject) accounts.get("accounts");
            if(jsonAccounts!=null){
                reqAccounts=new SearchRequestImpl();
                reqAccounts.setBase( new Dn( jsonAccounts.get("baseDN").toString() ));
                reqAccounts.setFilter(jsonAccounts.get("filter").toString() );
                String scopeAccounts = jsonAccounts.get("scope").toString();
                switch (scopeAccounts) {
                    case "subtree" ->reqAccounts.setScope(SearchScope.SUBTREE);
                    case "object" -> reqAccounts.setScope(SearchScope.OBJECT);
                    case "onelevel" ->reqAccounts.setScope(SearchScope.ONELEVEL);
                }

                JSONArray attributesAccounts = (JSONArray)jsonAccounts.get("attributes");
                for (Object attAcc : attributesAccounts)
                {
                    reqAccounts.addAttributes(attAcc.toString());
                }
            }
        }
        catch(Exception e) {
            System.out.println("Set accounts filter exception: "+e);
        }
    }
    private void setReqGroups(JSONObject groups){
        try {
            /**************** parse Groups from json ************/
            JSONObject jsonGroups = (JSONObject) groups.get("groups");
            if (jsonGroups != null) {
                reqGroups = new SearchRequestImpl();
                reqGroups.setBase(new Dn((String) jsonGroups.get("baseDN")));
                reqGroups.setFilter((String) jsonGroups.get("filter"));
                String scopeGroups = (String) jsonGroups.get("scope");
                switch (scopeGroups) {
                    case "subtree"->reqGroups.setScope(SearchScope.SUBTREE);
                    case "object"-> reqGroups.setScope(SearchScope.OBJECT);
                    case "onelevel"->reqGroups.setScope(SearchScope.ONELEVEL);
                }

                JSONArray attributesGroups = (JSONArray) jsonGroups.get("attributes");
                for (Object attG : attributesGroups) {
                    reqGroups.addAttributes(attG.toString());
                }
            }

        }
        catch(Exception e) {
            System.out.println("Set groups filter exception: "+e);
        }
    }
    public void readJsonConfig(){
        JSONParser parser = new JSONParser();
        try {
            Object obj = parser.parse(new FileReader(pathJson));

            JSONObject jsonObject = (JSONObject)obj;
            try{
                socketClient = new socketClient(new URI((String)jsonObject.get("uri")));
            }
            catch (URISyntaxException e) {
                System.out.println("socketClient not connected"+e);
            }
            page=Integer.parseInt(jsonObject.get("pageSize").toString());
            setReqAccounts(jsonObject);
            setReqGroups(jsonObject);
        } catch(IOException | ParseException e) {
           System.out.println("File or parse exception: "+e);
        }
    }
    public void setConnectionLdap(){
         connection = new LdapNetworkConnection( "localhost", 389 );
        try{
            connection.bind( "cn=admin,dc=monokee,dc=local", "admin" );
        }
        catch (LdapException e) {
            System.out.println("Connection not bind "+e);
        }
    }

    public void openConnectionSocketClient(){
        socketClient.connect();
    }
    public void searchOperation(){
        if(socketClient.isClosed()){
            socketClient.reconnect();
        }
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        searchOperationByReq(reqGroups, "groups");
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        searchOperationByReq(reqAccounts, "accounts");
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        socketClient.close();
    }
    private void searchOperationByReq(SearchRequest reqSearch,String nameTypeSearch){

        if(reqSearch!=null){
            // Create the control, and tell it we want N entries for every call
            PagedResults pagedControl = new PagedResultsImpl();
            pagedControl.setSize( page );

            try{
                while (true){
                    reqSearch.addControl(pagedControl);
                    SearchCursor searchResults = connection.search( reqSearch );

                    parseToJsonApi jsn=new parseToJsonApi();
                    jsn.parseJson(searchResults,nameTypeSearch);
                    socketClient.sendMessage(jsn.getJson());
                    // Now check the returned controls
                    Map<String, Control> controls =  searchResults.getSearchResultDone().getControls();

                    // We should get a PagedResult response
                    PagedResults responseControl = ( PagedResults ) controls.get( PagedResults.OID );

                    if ( responseControl != null )
                    {
                        // check if this is over, ie the cookie is empty
                        byte[] cookie = responseControl.getCookie();

                        if ( Strings.isEmpty( cookie ) )
                        {
                            // Ok, we are done
                            break;
                        }

                        // Prepare the next iteration, sending a bad cookie
                        pagedControl.setCookie( cookie );
                    }
                    else
                    {
                        break;
                    }
                }
            }
            catch (LdapException | CursorException e){
                System.out.println("Error during search: "+e);
            }
        }
    }
    public void closeConnectionLdap()  {
        try{
            connection.unBind();
            connection.close();
        }
        catch (LdapException | IOException e) {
            System.out.println("Error in close connection "+e);
        }
    }
    public static void main(String[] args) throws URISyntaxException, InterruptedException, CursorException, LdapException, IOException {
        final ldapApi apiL = new ldapApi();
        apiL.readJsonConfig();
        apiL.openConnectionSocketClient();
      //  apiL.setConnection();

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                apiL.setConnectionLdap();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                apiL.searchOperation();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                apiL.closeConnectionLdap();
            }
        }, 0, 25000);


        // apiL.addUSer();
       // apiL.modifyUser();
       // apiL.deleteUser();
    }
}
