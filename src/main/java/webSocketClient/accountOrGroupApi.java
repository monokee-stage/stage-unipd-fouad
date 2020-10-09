package webSocketClient;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;

import java.util.Vector;

public class accountOrGroupApi {
    //general
    private String cn;
    private String sn;

    //account
    private String mail;
    private String telephoneNumber;
    private String postalAddress;
    //group
    private Vector<String> member;


    public accountOrGroupApi(Entry result){
        setCn(result);
        setSn(result);
        setMail(result);
        setTelephoneNumber(result);
        setPostalAddress(result);
        setMember(result);
    }

    private void setCn(Entry r){
        if(r.get("cn")!=null){
            this.cn=r.get("cn").get().getString();
        }
    }
    private void setSn(Entry r){
        if(r.get("sn")!=null){
            this.sn=r.get("sn").get().getString();
        }
    }
    private void setMail(Entry r){
        if(r.get("mail")!=null){
            this.mail=r.get("mail").get().getString();
        }
    }
    private void setTelephoneNumber(Entry r){
        if(r.get("telephoneNumber")!=null){
            this.telephoneNumber=r.get("telephoneNumber").get().getString();
        }
    }
    private void setPostalAddress(Entry r){
        if(r.get("postalAddress")!=null){
            this.postalAddress=r.get("postalAddress").get().getString();

        }
    }
    private void setMember(Entry r){
        if(r.get("member")!=null){
            member=new Vector<>();
            for ( Value value : r.get("member") )
            {
                this.member.add(value.getString());
            }
        }
    }
}
