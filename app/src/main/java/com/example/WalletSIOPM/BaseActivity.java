package com.example.WalletSIOPM;

public abstract class BaseActivity extends SecureActivity {

    protected abstract int getSelectedNavId();

    protected void setupBottomNav() {
        NavigationHelper.setup(this, getSelectedNavId());
    }
}