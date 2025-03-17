import pandas as pd
import sys
from pathlib import Path

def process_excel(input_file):
    """Process Excel file to improve data structure."""
    # Read all sheets from the Excel file
    xls = pd.ExcelFile(input_file)
    
    # Create a new Excel writer
    output_file = str(Path(input_file).with_name('processed_' + Path(input_file).name))
    writer = pd.ExcelWriter(output_file, engine='openpyxl')
    
    for sheet_name in xls.sheet_names:
        # Read each sheet
        df = pd.read_excel(input_file, sheet_name=sheet_name)
        
        # Check if this is a key-value sheet (2 columns)
        if len(df.columns) == 2:
            # Already in key-value format, just clean up
            df.columns = ['Field', 'Value']
            df = df.dropna(how='all')
        else:
            # For regular tables, improve formatting
            # Remove empty rows and columns
            df = df.dropna(how='all', axis=0).dropna(how='all', axis=1)
            
            # Convert date-like columns
            for col in df.columns:
                if df[col].dtype == 'object':
                    try:
                        pd.to_datetime(df[col], errors='raise')
                        df[col] = pd.to_datetime(df[col]).dt.strftime('%Y-%m-%d')
                    except:
                        pass
                        
            # Format numeric columns
            numeric_cols = df.select_dtypes(include=['float64', 'int64']).columns
            if not numeric_cols.empty:
                df[numeric_cols] = df[numeric_cols].round(2)
        
        # Write the processed dataframe
        df.to_excel(writer, sheet_name=sheet_name, index=False)
        
        # Auto-adjust columns width
        worksheet = writer.sheets[sheet_name]
        for idx, col in enumerate(df.columns):
            max_length = max(
                df[col].astype(str).apply(len).max(),
                len(str(col))
            )
            worksheet.column_dimensions[chr(65 + idx)].width = max_length + 2
    
    writer.close()
    return output_file

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python excel_processor.py <excel_file>")
        sys.exit(1)
    
    input_file = sys.argv[1]
    try:
        output_file = process_excel(input_file)
        print(f"Processed file saved as: {output_file}")
    except Exception as e:
        print(f"Error processing file: {e}")
        sys.exit(1) 