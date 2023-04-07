package com.tapacross.sns.crawler.instagram.keyword;

public class instagramKeywordDataVO {
    String keyword;
    String pageId;
    String proxyJson;

    boolean contiune;

    public boolean isContiune() {
        return contiune;
    }

    public void setContiune(boolean contiune) {
        this.contiune = contiune;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getPageId() {
        return pageId;
    }

    public void setPageId(String pageId) {
        this.pageId = pageId;
    }

    public String getProxyJson() {
        return proxyJson;
    }

    public void setProxyJson(String proxyJson) {
        this.proxyJson = proxyJson;
    }

    @Override
    public String toString() {
        return "instagramKeywordDataVO{" +
                "keyword='" + keyword + '\'' +
                ", pageId='" + pageId + '\'' +
                ", proxyJson='" + proxyJson + '\'' +
                ", contiune=" + contiune +
                '}';
    }
}
