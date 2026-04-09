package com.pience.padobox.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "padobox")
public class DefaultConfig {

    private String connectType;
    private String kakaoSendYn;
    private String kakaoPatnerSendYn;
    private String restVersion;
    private String headerRestApi;
    private String headerSessionTokenDev;
    private String headerSessionTokenProd;
    private String moimapiUrlStage;
    private String moimapiAuthStage;
    private String moimapiUrlProd;
    private String moimapiAuthProd;
    private String padoboxJwtSecretDev;
    private String padoboxJwtSecretProd;
    private int moimApiOrderListLimit;
    private int moimApiSelleridListLimit;
    private int listLimit;
    private int accountListLimit;
    private String groupIdStage;
    private String groupIdProd;
    private String topSelleridStage;
    private String topSelleridProd;
    private String userInfoSeed;
    private String lastIdxSeed;
    private String kakaoRestUrl;
    private String kakaoUserid;
    private String kakaoProfileKey;
    private String failSendGmailAddr;
    private String failSendGmailPw;
    private String failReciverAddr;
    private int batchSize;
}