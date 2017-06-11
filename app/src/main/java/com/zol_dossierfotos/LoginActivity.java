// Package name.
package com.zol_dossierfotos;

// Imports from official sources.
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.net.InetAddress;
import java.net.UnknownHostException;

// Import external sources(UnboundID classes).
import com.unboundid.ldap.sdk.BindResult;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.ldap.sdk.SimpleBindRequest;

public class LoginActivity extends AppCompatActivity {

    // Edit these for change in GroupName or change in ServerLinks.
    private static final String GROUP_CN = "groupName";
    private static final String AD_SERVER2 = "ad_server2";
    private static final String AD_SERVER3 = "ad_server3";
    private static final String AD_SERVER4 = "ad_server4";

    // Handles starting of the activity.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the layout.
        setContentView(R.layout.activity_login);

        // Get our 2 TextField instances.
        final EditText username = (EditText) this.findViewById(R.id.username_input);
        final EditText password = (EditText) this.findViewById(R.id.password_input);

        // Get our LoginButton instance.
        final Button login = (Button) this.findViewById(R.id.login_button);

        // Set a ClickListener for when the user presses "done" on the keyboard.
        password.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    login.callOnClick();
                }
                return false;
            }
        });

        // Set a listener for the LoginButton.
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Get the values from the fields.
                String usernameString = username.getText().toString();
                String passwordString = password.getText().toString();

                // Check the user filled in both fields.
                if (usernameString.isEmpty() || passwordString.isEmpty()) {
                    // The users credentials are empty.
                    new AlertDialog.Builder(LoginActivity.this, R.style.AlertDialogTheme)
                            .setMessage("Vul uw gebruikersnaam en wachtwoord in a.u.b. " +
                                    "(Gebruikersnaam is uw ZOL mailadres)")
                            .setPositiveButton("Ok",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog,
                                                            int id) {
                                            dialog.dismiss();
                                        }
                                    })
                            .create()
                            .show();

                } else
                    // Check the user in the AD.
                    // Here we give the sAMAccountName, password, group CN and ADs.
                    new checkLogin().execute(usernameString, passwordString,
                            GROUP_CN, AD_SERVER2, AD_SERVER3, AD_SERVER4);

            }
        });

    }

    // This AsyncTask handles the asynchronous processing of the login.
    private class checkLogin extends AsyncTask<String, Void, String> {

        private LDAPConnection ldapConnection = new LDAPConnection();

        // Handled on UI thread before execution of doInBackground.
        @Override
        protected void onPreExecute() {

        }

        // Threaded method.
        @Override
        protected String doInBackground(String... params) {

            BindResult bindResult;
            InetAddress address;
            String status = "";

            // Try to resolve the name for one of the services.
            try {
                try {
                    // Try to get the IP for the first DC.
                    address = InetAddress.getByName(params[3]);
                } catch(UnknownHostException host1) {
                    try {
                        // Try to get the IP for the second DC.
                        address = InetAddress.getByName(params[4]);
                    } catch(UnknownHostException host2) {
                        // Try to get the IP for the second DC.
                        address = InetAddress.getByName(params[5]);
                    }
                }
                // Get the IP-address for our service.
                String ipAddress = address.getHostAddress();
                // Setup a connection to the service.
                ldapConnection.connect(ipAddress, 389);
                // Parse the sAMAccountName to our desired format (without @zol.be).
                String sAMAccountName;
                int index = params[0].indexOf('@');
                if (index==-1) {
                    sAMAccountName = params[0];
                } else {
                    sAMAccountName = params[0].substring(0,index);
                }
                // Check the users credentials on the service.
                bindResult = ldapConnection.bind(new SimpleBindRequest(
                        sAMAccountName + "@zol.be", params[1]));
                // If the credentials are right then we check for the group entry else we return
                // "wrong credentials".
                if (bindResult.getResultCode() == ResultCode.SUCCESS) {
                    // Get the user entry for the sAMAccountName.
                    SearchResult searchResults = ldapConnection.search("dc=domain,dc=zol,dc=be",
                    SearchScope.SUB, "(sAMAccountName="+sAMAccountName+")");
                    String groupEntry;
                    // Check the entry was found.
                    if (searchResults.getEntryCount() > 0) {
                        // Get the entry.
                        SearchResultEntry entry = searchResults.getSearchEntries().get(0);
                        // Get the group "memberOf" attribute.
                        groupEntry = entry.getAttributeValue("memberOf");
                        // Check if he is in our group CN.
                        if (groupEntry.contains(params[2])) {
                            // return that the user is allowed.
                            status = "LOGIN";
                        } else
                            // return that the user doesn't have the rights.
                            status = "NOT A MEMBER OF GROUP";
                    }
                } else {
                    // return that the user his credentials are false.
                    status = "WRONG CREDENTIALS";
                }
                return status;
            } // This catch gets thrown when the service is available
            // but something is wrong with the credentials.
            catch (LDAPException e) {
                return "WRONG CREDENTIALS";
            } // This catch gets thrown when none of the 3 services is available.
            catch(UnknownHostException host3){
                ConnectivityManager cm = (ConnectivityManager)getApplicationContext()
                        .getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                boolean isConnected = activeNetwork != null &&
                        activeNetwork.isConnectedOrConnecting();

                if (isConnected) {
                    return "NO HOST AVAILABLE";
                } else
                    return "NO CONNECTION";
            }
        }

        // Handled on UI thread after execution of doInBackground.
        @Override
        protected void onPostExecute(String result) {

            ldapConnection.close();

            switch (result) {
                // The user authenticated successfully.
                // Launch the MainActivity.
                case "LOGIN":

                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();

                    break;

                // The users credentials didn't contain the group entry.
                // Launch the appropriate dialog.
                case "NOT A MEMBER OF GROUP":

                    new AlertDialog.Builder(LoginActivity.this, R.style.AlertDialogTheme)
                            .setMessage("U beschikt niet over de juiste machtiging " +
                                    "voor de applicatie te gebruiken.")
                            .setPositiveButton("Ok",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog,
                                                            int id) {
                                            dialog.dismiss();
                                        }
                                    })
                            .create()
                            .show();

                    break;

                // The user authenticated unsuccessfully.
                // Launch the appropriate dialog.
                case "WRONG CREDENTIALS":

                    new AlertDialog.Builder(LoginActivity.this, R.style.AlertDialogTheme)
                            .setMessage("Foutieve logingegevens, controleer uw gebruikersnaam " +
                                    "en wachtwoord. (Gebruikersnaam is uw ZOL mailadres)")
                            .setPositiveButton("Ok",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog,
                                                            int id) {
                                            dialog.dismiss();
                                        }
                                    })
                            .create()
                            .show();

                    break;

                // None of the services are available.
                // Launch the appropriate dialog.
                case "NO HOST AVAILABLE":

                    new AlertDialog.Builder(LoginActivity.this, R.style.AlertDialogTheme)
                            .setMessage("Geen service beschikbaar.")
                            .setPositiveButton("Ok",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog,
                                                            int id) {
                                            dialog.dismiss();
                                        }
                                    })
                            .create()
                            .show();

                    break;

                // No connection to wifi available.
                // Launch the appropriate dialog.
                case "NO CONNECTION":

                    new AlertDialog.Builder(LoginActivity.this, R.style.AlertDialogTheme)
                            .setMessage("Geen verbinding, controleer of " +
                                    "uw Wi-Fi connectie ingeschakeld en ingesteld is.")
                            .setPositiveButton("Ok",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog,
                                                            int id) {
                                            dialog.dismiss();
                                        }
                                    })
                            .create()
                            .show();

                    break;

                default:

                    break;

            }

        }

        @Override
        protected void onProgressUpdate(Void... values) {

        }

    }

}