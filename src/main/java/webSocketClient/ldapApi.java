package webSocketClient;

import com.google.gson.Gson;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.entry.*;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.*;
import org.apache.directory.api.ldap.model.name.Dn;
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
    Gson g=new Gson();
    socketClient socketClient;
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
                    case "subtree" -> reqAccounts.setScope(SearchScope.SUBTREE);
                    case "object" -> reqAccounts.setScope(SearchScope.OBJECT);
                    case "onelevel" -> reqAccounts.setScope(SearchScope.ONELEVEL);
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
                    case "subtree" -> reqGroups.setScope(SearchScope.SUBTREE);
                    case "object" -> reqGroups.setScope(SearchScope.OBJECT);
                    case "onelevel" -> reqGroups.setScope(SearchScope.ONELEVEL);
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
            setReqAccounts(jsonObject);
            setReqGroups(jsonObject);
        } catch(IOException | ParseException e) {
           System.out.println("File or parse exception: "+e);
        }
    }
    public void setConnection(){
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
        try{
//(&(objectClass=person)(memberOf=cn=developers,ou=groups,dc=monokee,dc=local))
            // Process the request
            if(reqGroups!=null){
                SearchCursor searchGroups = connection.search(reqGroups);
                String jsonResults="{ \"groups\": { ";
                while ( searchGroups.next() )
                {
                    Response response = searchGroups.get();

                    // process the SearchResultEntry
                    if ( response instanceof SearchResultEntry)
                    {
                        Entry resultEntry = ( ( SearchResultEntry ) response ).getEntry();

                        accountOrGroupApi a =new accountOrGroupApi(resultEntry);
                        System.out.println( "-------JSON--------");
                        System.out.println( g.toJson(a));

                        socketClient.sendMessage(g.toJson(a));
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            if(reqAccounts!=null){
                SearchCursor searchAccounts = connection.search(reqAccounts);

                while ( searchAccounts.next() )
                {
                    Response response = searchAccounts.get();

                    // process the SearchResultEntry
                    if ( response instanceof SearchResultEntry)
                    {
                        Entry resultEntry = ( ( SearchResultEntry ) response ).getEntry();

                        accountOrGroupApi a =new accountOrGroupApi(resultEntry);
                        System.out.println( "-------JSON--------");
                        System.out.println( g.toJson(a));

                        socketClient.sendMessage(g.toJson(a));
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            socketClient.close();
        }
        catch (LdapException | CursorException e) {
            System.out.println("Error in search "+e);
        }
    }
    public void addUSer() {
        try{
            connection.add(
                    new DefaultEntry(
                            "cn=test,ou=people,dc=monokee,dc=local",
                            "objectClass: inetOrgPerson",
                            "objectClass: organizationalPerson",
                            "objectClass: person",
                            "objectClass: top",
                            "postalAddress: via roma 5.i9",
                            "cn: test",
                            "sn: testProva") );
        }catch (LdapException e){
            System.out.println("Error in add "+e);
        }

    }
    public void modifyUser(){
        try{
            Modification addedGivenName = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, "telephoneNumber",
                "sssss" );

            connection.modify( "cn=testadd,ou=people,dc=monokee,dc=local", addedGivenName );
        }catch (LdapException e){
            System.out.println("Error in modify "+e);
        }
    }
    public void deleteUser()  {
        try{
            connection.delete("cn=testadd,ou=people,dc=monokee,dc=local");
        }
        catch (LdapException e) {
            System.out.println("Error in delete user "+e);
        }
    }
    public void closeConnection()  {
        try{
            connection.unBind();
            connection.close();
        }
        catch (LdapException | IOException e) {
            System.out.println("Error in close connection "+e);
        }
    }

    public static void main(String[] args) throws URISyntaxException, InterruptedException {
        final ldapApi apiL = new ldapApi();
        apiL.readJsonConfig();
        apiL.openConnectionSocketClient();

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                apiL.setConnection();
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
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                apiL.closeConnection();
            }
        }, 0, 15000);

       // apiL.addUSer();
       // apiL.modifyUser();
       // apiL.deleteUser();
    }
}
