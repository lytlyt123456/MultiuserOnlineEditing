package com.example.multiuser_online_editing.service.document_management;

import com.example.multiuser_online_editing.entity.document_management.Document;
import com.huaban.analysis.jieba.JiebaSegmenter;
import com.huaban.analysis.jieba.SegToken;

import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.DoublePoint;
import org.apache.commons.math3.ml.distance.EuclideanDistance;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

public class Classification_AI {
    private static JiebaSegmenter segmenter = new JiebaSegmenter(); // 中文分词器

    public static List<DocumentClass> classification_AI(List<Document> accessibleDocuments, int numOfClusters) {
        if (accessibleDocuments.isEmpty()) {
            return new ArrayList<>();
        }

        // 文本预处理
        List<String> words = preprocessDocuments(accessibleDocuments);

        List<List<Double>> documentVectors = computeTFIDFScore(accessibleDocuments, words);

        return performKMeans(accessibleDocuments, documentVectors, words, numOfClusters, 50);
    }

    private static List<String> preprocessDocuments(List<Document> documents) {
        StringBuilder contentBuilder = new StringBuilder();
        for (Document document: documents) {
            String documentContent = document.getContent().length() > 300
                    ? document.getContent().substring(0, 300) : document.getContent();
            contentBuilder.append(document.getTitle()).append(" ").append(documentContent).append(" ");
        }

        String content = contentBuilder.toString();

        // 转小写，并移除HTML标签
        String cleanContent = content.toLowerCase().replaceAll("<[^>]+>", " ");

        // jieba分词
        List<SegToken> segTokens = segmenter.process(cleanContent, JiebaSegmenter.SegMode.SEARCH);
        List<String> words = new ArrayList<>();
        for (SegToken segToken: segTokens) {
            words.add(segToken.word);
        }

        // 去除停用词
        Set<String> stopWords = getStopWords();
        List<String> cleanWords = new ArrayList<>();

        for (String word: words) {
            String cleanWord = word.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", "");
            // \u4e00-\u9fa5是常用汉字的unicode码

            if (cleanWord.length() > 1 && !stopWords.contains(cleanWord) && !isNumeric(cleanWord)) {
                cleanWords.add(cleanWord);
            }
        }

        // 去重处理
        cleanWords = new ArrayList<>(new HashSet<>(cleanWords));

        return cleanWords;
    }

    private static boolean isNumeric(String str) {
        return str.matches("[0-9]+(.[0-9]*)?");
    }

    private static Set<String> getStopWords() {
        return Set.of("the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for",
                "of", "with", "by", "is", "are", "was", "were", "be", "been", "being",
                "this", "that", "these", "those", "i", "you", "he", "she", "it", "we", "they",
                "的", "了", "在", "是", "我", "有", "和", "就", "不", "人", "都", "一", "一个",
                "上", "也", "很", "到", "说", "要", "去", "你", "会", "着", "没有", "看", "好",
                "自己", "这", "那", "他", "她", "它", "我们", "你们", "他们");
    }

    private static List<List<Double>> computeTFIDFScore(List<Document> documents, List<String> words) {
        List<List<Double>> documentVectors = new ArrayList<>();
        List<Double> idfScore = calculateIDF(documents, words);

        // 为每个文档计算TF-IDF向量
        for (Document doc: documents) {
            List<Double> tfidfVector = calculateTFIDFVector(doc, words, idfScore);
            documentVectors.add(tfidfVector);
        }

        return documentVectors;
    }

    private static List<Double> calculateIDF(List<Document> documents, List<String> words) {
        List<Double> idfScore = new ArrayList<>();
        int totalDocuments = documents.size();

        for (String word: words) {
            long documentsWithWord = 0;
            for (Document document: documents) {
                String documentContent = document.getContent().length() > 300
                        ? document.getContent().substring(0, 300) : document.getContent();
                String content = (document.getTitle() + " " + documentContent).toLowerCase();
                if (content.contains(word))
                    ++documentsWithWord;
            }

            // 平滑IDF计算，避免除零
            double idf = Math.log((double) totalDocuments / ((double) documentsWithWord + 1e-5)) + 1;
            idfScore.add(idf);
        }

        return idfScore;
    }

    private static List<Double> calculateTFIDFVector(Document document, List<String> words, List<Double> idfScore) {
        List<Double> vector = new ArrayList<>();
        String documentContent = document.getContent().length() > 300
                ? document.getContent().substring(0, 300) : document.getContent();
        String content = (document.getTitle() + " " + documentContent).toLowerCase();

        // 计算词频(TF)
        List<Integer> wordFrequency = new ArrayList<>();
        int totalWords = 0;

        for (String word: words) {
            int count = 0;
            int index = 0;
            while ((index = content.indexOf(word, index)) != -1) {
                ++count;
                index += word.length();
            }

            wordFrequency.add(count);
            totalWords += count;
        }

        // 计算TF-IDF
        for (int i = 0; i < words.size(); ++i) {
            int freq = wordFrequency.get(i);
            double tf = totalWords > 0 ? (double) freq / totalWords : 0;
            double idf = idfScore.get(i);
            double tfidf = tf * idf;
            vector.add(tfidf);
        }

        return vector;
    }

    private static List<DocumentClass> performKMeans(
            List<Document> documents, List<List<Double>> vectors,
            List<String> words, int k, int maxIterations
    ) {
        // 转换为KMeansPlusPlusClusterer需要的格式
        List<DoublePoint> points = new ArrayList<>();
        for (List<Double> vector : vectors) {
            double[] array = vector.stream()
                    .mapToDouble(Double::doubleValue)
                    .toArray();
            points.add(new DoublePoint(array));
        }

        // 创建聚类器
        KMeansPlusPlusClusterer<DoublePoint> clusterer =
                new KMeansPlusPlusClusterer<>(k, maxIterations, new EuclideanDistance());

        // 执行聚类
        List<CentroidCluster<DoublePoint>> clusters = clusterer.cluster(points);

        List<DocumentClass> res = new ArrayList<>();

        // 提取每个聚类中包含的文档编号
        List<List<Integer>> clusterAssignments = new ArrayList<>();
        for (int i = 0; i < clusters.size(); i++) {
            clusterAssignments.add(new ArrayList<>());
        }
        for (int i = 0; i < points.size(); ++i) {
            DoublePoint point = points.get(i);
            int clusterIndex = findClusterIndex(point, clusters);
            clusterAssignments.get(clusterIndex).add(i);
        }

        // 提取每个聚类的主题关键词
        List<List<String>> themes = new ArrayList<>();
        for (CentroidCluster<DoublePoint> cluster : clusters) {
            // 求聚类中心
            DoublePoint center = (DoublePoint) cluster.getCenter();
            List<Double> centerList = new ArrayList<>();
            for (double value : center.getPoint()) {
                centerList.add(value);
            }
            List<Integer> indices = argSort(centerList);
            List<String> theme = new ArrayList<>();
            for (int i = 0; i < Math.min(3, indices.size()); ++i)
                theme.add(words.get(indices.get(i)));
            themes.add(theme);
        }

        for (int i = 0; i < clusters.size(); ++i) {
            List<Document> clusterDocuments = new ArrayList<>();
            List<Integer> clusterDocumentsIndices = clusterAssignments.get(i);
            for (Integer index: clusterDocumentsIndices)
                clusterDocuments.add(documents.get(index));

            List<String> clusterThemeWords = themes.get(i);

            res.add(new DocumentClass(clusterDocuments, clusterThemeWords));
        }

        return res;
    }

    private static List<Integer> argSort(List<Double> list) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < list.size(); ++i) {
            indices.add(i);
        }

        indices.sort((i, j) -> Double.compare(list.get(j), list.get(i)));
        return indices;
    }

    private static int findClusterIndex(DoublePoint point, List<CentroidCluster<DoublePoint>> clusters) {
        int bestCluster = -1;
        double minDistance = Double.MAX_VALUE;
        EuclideanDistance distance = new EuclideanDistance();

        for (int i = 0; i < clusters.size(); i++) {
            DoublePoint center = (DoublePoint) clusters.get(i).getCenter();
            double dist = distance.compute(point.getPoint(), center.getPoint());
            if (dist < minDistance) {
                minDistance = dist;
                bestCluster = i;
            }
        }
        return bestCluster;
    }
}
