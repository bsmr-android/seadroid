package com.seafile.seadroid2;

import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;

import com.seafile.seadroid2.account.Account;
import com.seafile.seadroid2.account.AccountManager;


public class AccountsActivity extends FragmentActivity {

    @SuppressWarnings("unused")
    private static final String DEBUG_TAG = "StartActivity";

    public static final String SHARED_PREF_NAME = "latest_account";
    public static final String SHARED_PREF_SERVER_KEY = "com.seafile.seadroid.server";
    public static final String SHARED_PREF_EMAIL_KEY = "com.seafile.seadroid.email";
    public static final String SHARED_PREF_TOKEN_KEY = "com.seafile.seadroid.token";
    
    private static AccountsActivity accountsActivity;
    
    private ListView accountsView;

    private AccountManager accountManager;

    private AccountAdapter adapter;
    List<Account> accounts;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        Log.d(DEBUG_TAG, "AccountsActivity.onCreate is called");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start);

        accountsActivity = this;
        
        accountsView = (ListView) findViewById(R.id.account_list_view);

        accountManager = new AccountManager(this);

        Button addAccount = new Button(this);
        addAccount.setText(R.string.add_account);
        accountsView.addFooterView(addAccount, null, true);
        accountsView.setFooterDividersEnabled(false);
        adapter = new AccountAdapter(this);
        accountsView.setAdapter(adapter);
        addAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View btn) {
                new CreateAccountChoiceDialog().show(getSupportFragmentManager(), "Choose a server");
            }
        });
        accountsView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position,
                    long id) {
                
                Account account = accounts.get(position);
                startFilesActivity(account);
            }
        });
        registerForContextMenu(accountsView);
    }

    // Always reload accounts on resume, so that when user add a new account,
    // it will be shown.
    @Override
    public void onResume() {
        super.onResume();

        refreshView();
    }

    private void refreshView() {
        accounts = accountManager.getAccountList();
        // Log.d(DEBUG_TAG, "Load accounts num " + accounts.size());
        adapter.clear();
        for (Account a : accounts) {
            adapter.add(a);
        }
        adapter.notifyChanged();
    }

    private void writeToSharedPreferences(Account account) {
        
        SharedPreferences sharedPref = getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(SHARED_PREF_SERVER_KEY, account.server);
        editor.putString(SHARED_PREF_EMAIL_KEY, account.email);
        editor.putString(SHARED_PREF_TOKEN_KEY, account.token);
        editor.commit();
    }
    
    private void clearDataFromSharedPreferences(Account account) {
        
        SharedPreferences sharedPref = getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        String latest_server = sharedPref.getString(SHARED_PREF_SERVER_KEY, null);
        String latest_email = sharedPref.getString(SHARED_PREF_EMAIL_KEY, null);
        if (latest_server.equals(account.server) && latest_email.equals(account.email)) {
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(SHARED_PREF_SERVER_KEY, null);
            editor.putString(SHARED_PREF_EMAIL_KEY, null);
            editor.putString(SHARED_PREF_TOKEN_KEY, null);
            editor.commit();
        }
    }
    
    private void startFilesActivity(Account account) {
        Intent intent = new Intent(this, BrowserActivity.class);
        intent.putExtra("server", account.server);
        intent.putExtra("email", account.email);
        intent.putExtra("token", account.token);
        
        writeToSharedPreferences(account);
        
        startActivity(intent);
        finish();
    }

    private void startEditAccountActivity(Account account) {
        Intent intent = new Intent(this, AccountDetailActivity.class);
        intent.putExtra("server", account.server);
        intent.putExtra("email", account.email);
        intent.putExtra("isEdited", true);
        startActivity(intent);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.account_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        Account account;
        switch (item.getItemId()) {
        case R.id.edit:
            account = adapter.getItem((int)info.id);
            startEditAccountActivity(account);
            return true;
        case R.id.delete:
            account = adapter.getItem((int)info.id);
            accountManager.deleteAccount(account);
            
            clearDataFromSharedPreferences(account);
            
            refreshView();
            return true;
        default:
            return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        
        super.onBackPressed();
    }
    
    public static final int PRIVATE_SERVER = 0;
    public static final int SEACLOUD_CC = 1;
    public static final int CLOUD_SEAFILE_COM = 3;

    public static class CreateAccountChoiceDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            
            final Context context = SeadroidApplication.getAppContext();
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.choose_server);
            builder.setItems(R.array.choose_server_array,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent;
                            switch (which) {
                            case 0:
                                intent = new Intent(context, AccountDetailActivity.class);
                                startActivity(intent);
                                break;
                            case 1:
                                intent = new Intent(context, AccountDetailActivity.class);
                                intent.putExtra("server", "https://seacloud.cc");
                                startActivity(intent);
                                break;
                            case 2:
                                intent = new Intent(context, AccountDetailActivity.class);
                                intent.putExtra("server", "https://cloud.seafile.com");
                                startActivity(intent);
                                break;
                            default:
                                return;
                            }
                            accountsActivity.finish();
                        }
                    });

            return builder.create();
        }
    }
    
}
