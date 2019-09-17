package com.hechuangwu.opensourseframe.eventbus;

/**
 * Created by cwh on 2019/9/16 0016.
 * 功能:
 */
public class MessageEvent {

    private String msg;

    public MessageEvent(String msg) {
        this.msg = msg;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    @Override
    public String toString() {
        return "MessageEvent{" +
                "msg='" + msg + '\'' +
                '}';
    }
}
