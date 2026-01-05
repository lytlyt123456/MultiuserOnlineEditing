package com.example.multiuser_online_editing.service.document_management;

import com.example.multiuser_online_editing.entity.document_management.Document;

import java.util.List;

public class DocumentClass {
    private List<Document> documents; // 一个聚类中包含的文档
    private List<String> themeWords; // 一个聚类中的所有主题词

    public DocumentClass(List<Document> documents, List<String> themeWords) {
        this.documents = documents;
        this.themeWords = themeWords;
    }

    public List<Document> getDocuments() { return documents; }
    public List<String> getThemeWords() { return themeWords; }
    public void setDocuments(List<Document> documents) { this.documents = documents; }
    public void setThemeWords(List<String> themeWords) { this.themeWords = themeWords; }
}