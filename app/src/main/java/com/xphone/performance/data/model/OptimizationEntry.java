package com.xphone.performance.data.model;

/**
 * Một vấn đề hiệu năng đã phát hiện và xử lý trong quá trình phát triển app.
 *
 * Mỗi mục đi theo đúng quy trình: phát hiện (tool) -> triệu chứng -> nguyên nhân -> cách sửa
 * -> số đo trước / sau, kèm file mã nguồn liên quan để đối chiếu.
 */
public class OptimizationEntry {

    /** Nhóm vấn đề, khớp với 3 trọng tâm: giật lag, CPU, bộ nhớ (+ cấu hình build). */
    public enum Category {
        LAG("🌊 Lag"),
        CPU("⚡ CPU"),
        MEMORY("🧠 Bộ nhớ"),
        BUILD("📦 Build");

        public final String label;

        Category(String label) {
            this.label = label;
        }
    }

    public final Category category;
    public final String title;
    public final String tool;
    public final String symptom;
    public final String cause;
    public final String fix;
    public final String before;
    public final String after;
    public final String sourceFile;

    public OptimizationEntry(Category category, String title, String tool, String symptom,
                             String cause, String fix, String before, String after, String sourceFile) {
        this.category = category;
        this.title = title;
        this.tool = tool;
        this.symptom = symptom;
        this.cause = cause;
        this.fix = fix;
        this.before = before;
        this.after = after;
        this.sourceFile = sourceFile;
    }
}
