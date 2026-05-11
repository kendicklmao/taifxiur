package client.controller;

import com.google.gson.Gson;

import client.AppContext;
import client.service.AlertServiceImpl;
import client.service.IAlertService;
import shared.utils.GsonUtils;

public abstract class UserController {
    protected AppContext ctx = AppContext.getInstance();
    protected IAlertService alertService = new AlertServiceImpl();
    protected Gson gson = GsonUtils.createGson();
}
