package com.example.multiuser_online_editing.service.document_management;

import com.example.multiuser_online_editing.entity.document_management.Document;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

import com.huaban.analysis.jieba.JiebaSegmenter; // 中文分词器
import com.huaban.analysis.jieba.SegToken;

public class DocumentSearch_AI { // 基于TF-IDF和余弦相似度的智能搜索
    private static JiebaSegmenter segmenter = new JiebaSegmenter(); // 中文分词器

    public static List<Document> advancedSearch_AI(
            String content, List<Document> accessibleDocuments
            // content为用户键入的搜索内容，accessibleDocuments为可供搜索的文档
    ) {
        if (accessibleDocuments.isEmpty()) {
            return new ArrayList<>();
        }

        // 文本预处理
        List<String> searchTerms = preprocessText(content);

        if (searchTerms.isEmpty()) {
            // 如果没有有效搜索词，返回最近访问的文档
            return accessibleDocuments.stream()
                    .limit(10)
                    .collect(Collectors.toList());
        }

        // 计算TF-IDF并排序
        List<DocumentScore> scoredDocuments = calculateDocumentScores(accessibleDocuments, searchTerms, content.toLowerCase());

        return scoredDocuments.stream()
                .sorted((d1, d2) -> Double.compare(d2.score, d1.score))
                .limit(10)
                .map(docScore -> docScore.document)
                .collect(Collectors.toList());
    }

    private static List<String> preprocessText(String text) {
        if (text == null || text.trim().isEmpty()) { // trim方法为移除字符串两端的空白字符
            return new ArrayList<>();
        }

        // 转换为小写
        String lowerText = text.toLowerCase();

        // 移除HTML标签（针对富文本内容）
        String cleanText = lowerText.replaceAll("<[^>]+>", " ");

        // 分词
        List<SegToken> segTokens = segmenter.process(cleanText, JiebaSegmenter.SegMode.SEARCH);
        List<String> words = new ArrayList<>();
        for (SegToken segToken: segTokens) {
            words.add(segToken.word);
        }

        // 停用词过滤
        List<String> terms = new ArrayList<>();
        Set<String> stopWords = getStopWords();

        for (String word: words) {
            String cleanWord = word.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", "");
            // \u4e00-\u9fa5是常用汉字的unicode码

            if (cleanWord.length() > 1 && !stopWords.contains(cleanWord) && !isNumeric(cleanWord)) {
                terms.add(cleanWord);
            }
        }

        // 去重处理
        terms = new ArrayList<>(new HashSet<>(terms));

        return terms;
    }

    private static Set<String> getStopWords() {
        return Set.of("the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for",
                "of", "with", "by", "is", "are", "was", "were", "be", "been", "being",
                "this", "that", "these", "those", "i", "you", "he", "she", "it", "we", "they",
                "的", "了", "在", "是", "我", "有", "和", "就", "不", "人", "都", "一", "一个",
                "上", "也", "很", "到", "说", "要", "去", "你", "会", "着", "没有", "看", "好",
                "自己", "这", "那", "他", "她", "它", "我们", "你们", "他们");
    }

    private static boolean isNumeric(String str) {
        return str.matches("[0-9]+(.[0-9]*)?");
    }

    private static class DocumentScore {
        Document document;
        double score;

        DocumentScore(Document document, double score) {
            this.document = document;
            this.score = score;
        }
    }

    private static List<DocumentScore> calculateDocumentScores(List<Document> documents, List<String> searchTerms, String searchContent) {
        // 构建文档-词频矩阵
        Map<Document, Map<String, Double>> documentVectors = new HashMap<>();
        Map<String, Double> idfMap = calculateIDF(documents, searchTerms);

        // 为每个文档计算TF-IDF向量
        for (Document doc: documents) {
            Map<String, Double> tfidfVector = calculateTFIDFVector(doc, searchTerms, idfMap);
            documentVectors.put(doc, tfidfVector);
        }

        // 计算搜索查询的TF-IDF向量
        Map<String, Double> queryVector = calculateQueryVector(searchContent, searchTerms, idfMap);

        // 计算余弦相似度
        List<DocumentScore> scoredDocuments = new ArrayList<>();
        for (Document doc : documents) {
            double similarity = calculateCosineSimilarity(queryVector, documentVectors.get(doc));
            scoredDocuments.add(new DocumentScore(doc, similarity));
        }

        return scoredDocuments;
    }

    // 计算逆文档频率
    private static Map<String, Double> calculateIDF(List<Document> documents, List<String> terms) {
        Map<String, Double> idfMap = new HashMap<>();
        int totalDocuments = documents.size();

        for (String term: terms) {
            long documentsWithTerm = 0;
            for (Document document: documents) {
                String content = (document.getTitle() + " " + document.getContent()).toLowerCase();
                if (content.contains(term))
                    ++documentsWithTerm;
            }

            // 平滑IDF计算，避免除零
            double idf = Math.log((double) totalDocuments / ((double) documentsWithTerm + 1e-5)) + 1;
            idfMap.put(term, idf);
        }

        return idfMap;
    }

    // 计算TF-IDF向量
    private static Map<String, Double> calculateTFIDFVector(Document document, List<String> terms, Map<String, Double> idfMap) {
        Map<String, Double> vector = new HashMap<>();
        String content = (document.getTitle() + " " + document.getContent()).toLowerCase();

        // 计算词频(TF)
        Map<String, Integer> termFrequency = new HashMap<>();
        int totalTerms = 0;

        for (String term: terms) {
            int count = 0;
            int index = 0;
            while ((index = content.indexOf(term, index)) != -1) {
                ++count;
                index += term.length();
            }

            termFrequency.put(term, count);
            totalTerms += count;
        }

        // 计算TF-IDF
        for (String term: terms) {
            int freq = termFrequency.getOrDefault(term, 0);
            double tf = totalTerms > 0 ? (double) freq / totalTerms : 0;
            double idf = idfMap.getOrDefault(term, 1.0);
            double tfidf = tf * idf;
            vector.put(term, tfidf);
        }

        return vector;
    }

    // 计算查询向量
    private static Map<String, Double> calculateQueryVector(String searchContent, List<String> searchTerms, Map<String, Double> idfMap) {
        Map<String, Double> vector = new HashMap<>();

        // 计算词频(TF)
        Map<String, Integer> termFrequency = new HashMap<>();
        int totalTerms = 0;

        for (String term: searchTerms) {
            int count = 0;
            int index = 0;
            while ((index = searchContent.indexOf(term, index)) != -1) {
                ++count;
                index += term.length();
            }

            termFrequency.put(term, count);
            totalTerms += count;
        }

        // 计算TF-IDF
        for (String term: searchTerms) {
            int freq = termFrequency.getOrDefault(term, 0);
            double tf = totalTerms > 0 ? (double) freq / totalTerms : 0;
            double idf = idfMap.getOrDefault(term, 1.0);
            double tfidf = tf * idf;
            vector.put(term, tfidf);
        }

        return vector;
    }

    // 计算余弦相似度
    private static double calculateCosineSimilarity(Map<String, Double> vector1, Map<String, Double> vector2) {
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        // 计算点积和范数
        for (String term : vector1.keySet()) {
            double v1 = vector1.getOrDefault(term, 0.0);
            double v2 = vector2.getOrDefault(term, 0.0);
            dotProduct += v1 * v2;
            norm1 += v1 * v1;
        }

        for (double value : vector2.values()) {
            norm2 += value * value;
        }

        // 避免除零
        if (norm1 == 0 || norm2 == 0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
}
