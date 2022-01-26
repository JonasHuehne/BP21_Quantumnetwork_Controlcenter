package communicationList;

import java.util.ArrayList;
import java.util.regex.Pattern;


public interface CommunicationList {

    boolean insert(final String name, final String ipAddress, final int port, final String signatureKey);


    boolean delete (final String name);

    boolean updateName (final String oldName, final String newName);

    boolean updateIP (final String name, final String ipAddress);

    boolean updatePort (final String name, final int port);

    boolean updateSignatureKey (final String name, final String signatureKey);

    DbObject query (final String name);

    ArrayList<DbObject> queryAll ();

}
