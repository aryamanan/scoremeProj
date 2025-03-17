# PDF Data Extractor

This is my ScoreMe hackathon submission. A simple tool to extract tables from PDFs and convert them into Excel files. It's pretty straightforward to use - just upload a PDF and it'll give you back an Excel file with all the tables it finds.

## Requirements

### Backend
- Java 17 or higher
- Maven 3.6 or higher
- Spring Boot 3.2.3
- Apache PDFBox 3.0.1 (for PDF processing)
- Apache POI 5.2.5 (for Excel generation)

### Frontend
- Node.js 16 or higher
- npm 8 or higher
- React 18
- TypeScript
- Vite 5.4

## Quick Start

1. Clone the repo:
   ```bash
   git clone <your-repo-url>
   cd pdf-data-extractor
   ```

2. Set up and run the backend:
   ```bash
   # Install Maven dependencies
   ./mvnw clean install

   # Run the Spring Boot application
   ./mvnw spring-boot:run
   ```
   The backend will start on http://localhost:8080

3. Set up and run the frontend:
   ```bash
   # Navigate to frontend directory
   cd frontend

   # Install dependencies
   npm install

   # Start development server
   npm run dev
   ```
   The frontend will start on http://localhost:5173

4. Open http://localhost:5173 in your browser

## Test it out

Check out the `validation_material` folder - I've included some sample PDFs you can try it with:
- test3.pdf: A sample PDF with multiple tables
- Hackathon-2.docx: Project documentation
- pdf_extract.xlsx: Sample output showing the expected format

## Features
- Extracts tables from multi-page PDFs
- Handles both simple and complex table layouts
- Converts key-value pairs into structured Excel format
- Supports multiple tables per page
- Exports each table to a separate Excel sheet
- Maintains data formatting (numbers, dates, text)

## Dependencies in Detail

### Backend Dependencies (from pom.xml)
```xml
<dependencies>
    <!-- Spring Boot -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
        <version>3.2.3</version>
    </dependency>

    <!-- PDF Processing -->
    <dependency>
        <groupId>org.apache.pdfbox</groupId>
        <artifactId>pdfbox</artifactId>
        <version>3.0.1</version>
    </dependency>

    <!-- Excel Generation -->
    <dependency>
        <groupId>org.apache.poi</groupId>
        <artifactId>poi</artifactId>
        <version>5.2.5</version>
    </dependency>
    <dependency>
        <groupId>org.apache.poi</groupId>
        <artifactId>poi-ooxml</artifactId>
        <version>5.2.5</version>
    </dependency>
</dependencies>
```

### Frontend Dependencies (from package.json)
```json
{
  "dependencies": {
    "@emotion/react": "^11.11.4",
    "@emotion/styled": "^11.11.0",
    "@mui/icons-material": "^5.15.13",
    "@mui/material": "^5.15.13",
    "react": "^18.2.0",
    "react-dom": "^18.2.0"
  }
}
```

## Notes
- Works best with well-structured PDFs
- Handles multiple tables per page
- Exports each table to a separate Excel sheet
- Key-value pairs are structured in a two-column format
- Regular tables maintain their original structure
- Supports automatic column width adjustment
- Handles special characters and formatting

## Common Issues & Solutions

1. If npm install fails (happened many times to me):
   ```bash
   # Clear npm cache and try again
   npm cache clean --force
   npm install
   ```

Let me know if you run into any issues!
