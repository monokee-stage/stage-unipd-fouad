import org.identityconnectors.common.IOUtil;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.*;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import java.util.List;
import java.util.Vector;

public class connLdap {
    public static void main(String[] args) throws IOException {
        ConnectorInfoManagerFactory fact = ConnectorInfoManagerFactory.getInstance();
        File bundleDirectory = new File("C:\\Users\\farid\\Desktop\\fo\\Uni\\Identity Governance\\TEST\\connTest\\src\\bundle");
        URL url = IOUtil.makeURL(bundleDirectory, "net.tirasa.connid.bundles.ldap-1.5.4.jar");
        ConnectorInfoManager manager = fact.getLocalManager(url);

        ConnectorKey key = new ConnectorKey("net.tirasa.connid.bundles.ldap", "1.5.4", "net.tirasa.connid.bundles.ldap.LdapConnector");
        ConnectorInfo info = manager.findConnectorInfo(key);

        // From the ConnectorInfo object, create the default APIConfiguration.
        APIConfiguration apiConfig = info.createDefaultAPIConfiguration();

        // From the default APIConfiguration, retrieve the ConfigurationProperties.
        ConfigurationProperties properties = apiConfig.getConfigurationProperties();

        // Print out what the properties are (not necessary)
        List propertyNames = properties.getPropertyNames();
        for (Object propName : propertyNames) {
            ConfigurationProperty prop = properties.getProperty((String) propName);
          //  System.out.println("Property Name: " + prop.getName() + "\tProperty Type: " + prop.getType());
        }

       properties.setPropertyValue("host", "localhost");
        properties.setPropertyValue("port", 389);
        properties.setPropertyValue("principal", "cn=admin,dc=monokee,dc=local");
        properties.setPropertyValue("readSchema", true);


        String s="admin";
        GuardedString guardedString = new GuardedString(s.toCharArray());
        properties.setPropertyValue("credentials", guardedString);
        String[] bc = {"dc=monokee,dc=local"};
        properties.setPropertyValue("baseContexts", bc);
        String[] oC = {"groupOfNames"};
        properties.setPropertyValue("groupObjectClasses", oC);
        
        String[] uC = {"user"};
        properties.setPropertyValue("accountObjectClasses", uC);

        //properties.setPropertyValue("filterWithOrInsteadOfAnd", true);

        ConnectorFacade conn = ConnectorFacadeFactory.getInstance().newInstance(apiConfig);
       // conn.validate();

       // System.out.println(conn.schema());
        Filter leftFilter = FilterBuilder.equalTo(AttributeBuilder.build("__NAME__", "cn=developers,ou=groups,dc=monokee,dc=local"));
        //Filter rightFilter = FilterBuilder.equalTo(AttributeBuilder.build("objectClass", "groupOfNames"));

        Filter filter = FilterBuilder.and(leftFilter);
         Vector<ConnectorObject> results = new Vector<ConnectorObject>();
        ResultsHandler handler = obj -> {
            results.add(obj);
            return true;
        };
        conn.search(ObjectClass.GROUP, filter, handler,null);

        System.out.println("size: "+ results.size());
        for (ConnectorObject result : results) {
            System.out.println("result: " + result);
        }
        System.out.println("------------------------------");
        Vector<Filter> v=new Vector<Filter>();
        for(int i=0;i<results.get(0).getAttributeByName("member").getValue().size();i++) {
            System.out.println("members: " + results.get(0).getAttributeByName("member").getValue().get(i));
            Filter temp = FilterBuilder.equalTo(AttributeBuilder.build("__NAME__", results.get(0).getAttributeByName("member").getValue().get(i)));

            v.add(temp);

        }
        if(v!=null) {
            Filter ff = FilterBuilder.or(v);
            Vector<ConnectorObject> accountGroup = new Vector<ConnectorObject>();
            ResultsHandler h = obj -> {
                accountGroup.add(obj);
                return true;
            };
            conn.search(ObjectClass.ACCOUNT, ff, h, null);
            System.out.println("result: " + accountGroup);
        }

    }
}
