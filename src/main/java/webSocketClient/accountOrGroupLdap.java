package webSocketClient;
import org.identityconnectors.framework.common.objects.ConnectorObject;

import java.util.Vector;

public class accountOrGroupLdap {

    //general
    private String cn;
    private String sn;

    //account
    private String mail;
    private String telephoneNumber;
    private String postalAddress;
    //group
    private Vector<String> member;


    public accountOrGroupLdap(ConnectorObject result){
        setCn(result);
        setSn(result);
        setMail(result);
        setTelephoneNumber(result);
        setPostalAddress(result);
        setMember(result);
    }

    private void setCn(ConnectorObject cO){
        if(cO.getAttributeByName("cn")!=null){
            this.cn=cO.getAttributeByName("cn").getValue().get(0).toString();
        }
    }
    private void setSn(ConnectorObject cO){
        if(cO.getAttributeByName("sn")!=null){
            this.sn=cO.getAttributeByName("sn").getValue().get(0).toString();
        }
    }
    private void setMail(ConnectorObject cO){
        if(cO.getAttributeByName("mail")!=null){
            this.mail=cO.getAttributeByName("mail").getValue().get(0).toString();
        }
    }
    private void setTelephoneNumber(ConnectorObject cO){
        if(cO.getAttributeByName("telephoneNumber")!=null){
            this.telephoneNumber=cO.getAttributeByName("telephoneNumber").getValue().get(0).toString();
        }
    }
    private void setPostalAddress(ConnectorObject cO){
        if(cO.getAttributeByName("postalAddress")!=null){
            this.postalAddress=cO.getAttributeByName("postalAddress").getValue().get(0).toString();
        }
    }
    private void setMember(ConnectorObject cO){
        if(cO.getAttributeByName("member")!=null){
            member=new Vector<>();
            for (int i=0;i<cO.getAttributeByName("member").getValue().size();i++ )
            {
                this.member.add(cO.getAttributeByName("member").getValue().get(i).toString());
            }
        }
    }
}
