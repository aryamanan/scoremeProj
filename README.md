# PDF Data Extractor

Hey! 👋 This is a simple tool I made to extract tables from PDFs and convert them into Excel files. It's pretty straightforward to use - just upload a PDF and it'll give you back an Excel file with all the tables it finds.

## Quick Start

1. Clone the repo
2. Run the backend:
   ```bash
   ./mvnw spring-boot:run
   ```
3. Run the frontend:
   ```bash
   cd frontend
   npm install
   npm run dev
   ```
4. Open http://localhost:5173 in your browser

## Test it out

Check out the `validation_material` folder - I've included some sample PDFs you can try it with. Upload them and see how the tables get extracted into Excel.

## Tech Stack
- Backend: Spring Boot + Java
- Frontend: React + TypeScript
- PDF Processing: Apache PDFBox
- Excel Generation: Apache POI

## Notes
- Works best with well-structured PDFs
- Handles multiple tables per page
- Exports each table to a separate Excel sheet

Let me know if you run into any issues! 