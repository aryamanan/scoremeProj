package com.myproject.pdftableextractor.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableData {
    private List<String> headers;
    private List<Map<String, String>> rows;
    private int pageNumber;
} 