package client;

import com.google.gson.Gson;

import shared.utils.GsonUtils;

public abstract class UserController {
    protected final AppContext ctx = AppContext.getInstance();
    protected final IAlertService alertService = new AlertServiceImpl();
    protected final Gson gson = GsonUtils.createGson();
}
