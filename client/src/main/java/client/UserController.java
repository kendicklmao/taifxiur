package client;

import com.google.gson.Gson;

import shared.utils.GsonUtils;

public abstract class UserController {
    protected AppContext ctx = AppContext.getInstance();
    protected IAlertService alertService = new AlertServiceImpl();
    protected Gson gson = GsonUtils.createGson();
}
