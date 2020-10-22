package webSocketClient;


import org.identityconnectors.common.IOUtil;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.*;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import java.util.*;

public class connLdap {
    private ConnectorFacade conn;
    private socketClient sockClient;
    final private String pathJson="src/main/java/webSocketClient/accountGroupConfigLdap.json";
    private ConfigurationProperties properties;
    private int pageSize;

    public void setConnectionLdap() throws IOException {
        ConnectorInfoManagerFactory fact = ConnectorInfoManagerFactory.getInstance();
        File bundleDirectory = new File("connIdBundle");
        URL url = IOUtil.makeURL(bundleDirectory, "net.tirasa.connid.bundles.ldap-1.5.4.jar");
        ConnectorInfoManager manager = fact.getLocalManager(url);

        ConnectorKey key = new ConnectorKey("net.tirasa.connid.bundles.ldap", "1.5.4", "net.tirasa.connid.bundles.ldap.LdapConnector");
        ConnectorInfo info = manager.findConnectorInfo(key);

        // From the ConnectorInfo object, create the default APIConfiguration.
        APIConfiguration apiConfig = info.createDefaultAPIConfiguration();

        // From the default APIConfiguration, retrieve the ConfigurationProperties.
        properties = apiConfig.getConfigurationProperties();

        ResultsHandlerConfiguration resConf = apiConfig.getResultsHandlerConfiguration();
        resConf.setEnableFilteredResultsHandler(true);
        resConf.setFilteredResultsHandlerInValidationMode(true);
        // Print out what the properties are (not necessary)
        List propertyNames = properties.getPropertyNames();
        for (Object propName : propertyNames) {
            ConfigurationProperty prop = properties.getProperty((String) propName);
        }
        setProperties();

         conn = ConnectorFacadeFactory.getInstance().newInstance(apiConfig);

        // conn.validate();
    }
    public void openConnectionSocketClient(){

        try{
            JSONParser parser = new JSONParser();

            Object obj = parser.parse(new FileReader(pathJson));

            JSONObject jsonObject = (JSONObject)obj;

            sockClient = new socketClient(new URI((String)jsonObject.get("uri")));
            sockClient.connect();
        }
        catch (URISyntaxException | FileNotFoundException e) {
            System.out.println("socketClient not connected"+e);
        } catch (ParseException | IOException e) {
            e.printStackTrace();
        }

    }
    public void setProperties (){

        properties.setPropertyValue("host", "localhost");
        properties.setPropertyValue("port", 389);
        properties.setPropertyValue("principal", "cn=admin,dc=monokee,dc=local");
        properties.setPropertyValue("readSchema", true);
       // properties.setPropertyValue("useBlocks",true);
        String s="admin";
        GuardedString guardedString = new GuardedString(s.toCharArray());
        properties.setPropertyValue("credentials", guardedString);
        JSONParser parser = new JSONParser();
        try {
            Object obj = parser.parse(new FileReader(pathJson));

            JSONObject jsonObject = (JSONObject)obj;

            // baseDN
            String[] bc = {(String)jsonObject.get("baseDN")};
            properties.setPropertyValue("baseContexts", bc);
            pageSize=Integer.parseInt(jsonObject.get("pageSize").toString());
            // search where objectClass=groupOfNames for only group
            JSONObject jsonGroups= (JSONObject) jsonObject.get("groups");
            JSONArray objectClassGroup = (JSONArray)jsonGroups.get("objectClassFilter");
            int i = 0;
            if(objectClassGroup.size()!=0) {
                String[] oC = new String[objectClassGroup.size()];

                for (Object objCGroup : objectClassGroup) {
                    oC[i] = objCGroup.toString();
                    i++;
                }
                properties.setPropertyValue("groupObjectClasses", oC);
            }
            // search where objectClass=inetOrgPerson for only accounts
            JSONObject jsonAccount= (JSONObject) jsonObject.get("accounts");
            JSONArray objectClassAccounts = (JSONArray)jsonAccount.get("objectClassFilter");

            if(objectClassAccounts.size()!=0) {
                String[] uC = new String[objectClassAccounts.size()];
                i = 0;
                for (Object objAcc : objectClassAccounts) {
                    uC[i] = objAcc.toString();
                    i++;
                }
                properties.setPropertyValue("accountObjectClasses", uC);
            }
        } catch(IOException | ParseException e) {
            System.out.println("File or parse exception: "+e);
        }
    }
    public void search()  {
        if(sockClient.isClosed()){
            sockClient.reconnect();
        }
        searchAccountOrGroup("groups");
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        searchAccountOrGroup("accounts");
        sockClient.close();

    }
    private void searchAccountOrGroup(String nameTypeSearch)  {
        System.out.println("**************************************");

        Map<String, Object> searchOpt = new HashMap<String, Object>();
        searchOpt.put(OperationOptions.OP_PAGE_SIZE,pageSize);
        searchOpt.put(OperationOptions.OP_PAGED_RESULTS_COOKIE,null);
        while (true){
            Vector<ConnectorObject> accountGroup = new Vector<ConnectorObject>();
            ResultsHandler accountHandler = obj -> {
                accountGroup.add(obj);
                return true;
            };
            System.out.println("------------------------------");

            OperationOptions searchOptions = new OperationOptions(searchOpt);
            SearchResult searchResults=null;
            if (nameTypeSearch.equals("groups")) {
                searchResults = conn.search(ObjectClass.GROUP, null, accountHandler, searchOptions);
            }
            else   if(nameTypeSearch.equals("accounts")){
                searchResults = conn.search(ObjectClass.ACCOUNT, null, accountHandler, searchOptions);
            }

            if(searchResults != null){
                parseToJsonConnId jsn=new parseToJsonConnId();
                try{
                    jsn.parseJson(accountGroup,nameTypeSearch);
                }
                catch (Exception e){
                  System.out.println("Error in the parse: "+e);
                }
                sockClient.sendMessage(jsn.getJson());
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("iterazioni: "+accountGroup);
                System.out.println(searchResults.getPagedResultsCookie());
                System.out.println(searchResults.getRemainingPagedResults());
                if(searchResults.getPagedResultsCookie()==null){
                    break;
                }
                searchOpt.replace(OperationOptions.OP_PAGED_RESULTS_COOKIE,searchResults.getPagedResultsCookie());

            }
            else {
                break;
            }
        }
    }

    private Uid findUid(String dn, String type) {
        Filter leftFilter = FilterBuilder.equalTo(AttributeBuilder.build("__NAME__", dn));
        Filter filter = FilterBuilder.and(leftFilter);
        Vector<ConnectorObject> results = new Vector<ConnectorObject>();
        ResultsHandler handler = obj -> {
            results.add(obj);
            return true;
        };
        if(type.equals("account")){
            conn.search(ObjectClass.ACCOUNT, filter, handler,null);
        }
        else if (type.equals("group")){
            conn.search(ObjectClass.GROUP, filter, handler,null);
        }
        return  results.get(0).getUid();
    }
    private void closeConnection() {

    }
    public static void main(String[] args)  {
      connLdap ldapConn=new connLdap();
      ldapConn.openConnectionSocketClient();


        Timer timer = new Timer();
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                try {
                    ldapConn.setConnectionLdap();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                ldapConn.search();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                ldapConn.closeConnection();
            }
        }, 0, 25000);


    }
}
