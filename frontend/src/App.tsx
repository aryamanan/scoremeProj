import { useState } from 'react'
import { 
  Container, 
  Paper, 
  Typography, 
  Box, 
  Alert, 
  Snackbar, 
  Button,
  CircularProgress,
  Divider
} from '@mui/material'
import { ThemeProvider, createTheme } from '@mui/material/styles'
import CssBaseline from '@mui/material/CssBaseline'
import DownloadIcon from '@mui/icons-material/Download'
import PDFUploader from './components/PDFUploader'
import TableDisplay from './components/TableDisplay'

const theme = createTheme({
  palette: {
    mode: 'light',
    primary: {
      main: '#1976d2',
    },
    secondary: {
      main: '#dc004e',
    },
  },
})

interface BankStatement {
  headers: string[];
  rows: Record<string, string>[];
  pageNumber: number;
}

function App() {
  const [tableData, setTableData] = useState<BankStatement[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [processingStatus, setProcessingStatus] = useState<string>('')
  const [excelBlob, setExcelBlob] = useState<Blob | null>(null)
  const [accountInfo, setAccountInfo] = useState<Record<string, string>>({})

  const extractAccountInfo = (data: BankStatement[]) => {
    const accountDetails: Record<string, string> = {};
    try {
      const firstPage = data.find(page => page.pageNumber === 1);
      
      if (firstPage && firstPage.rows) {
        firstPage.rows.forEach(row => {
          if (!row) return;
          
          const values = Object.values(row);
          if (!values || values.length === 0) return;
          
          const value = values[0];
          if (!value) return;

          if (value.includes('Account No')) {
            const parts = value.split(':');
            if (parts.length > 1) {
              accountDetails['Account Number'] = parts[1].trim();
            }
          } else if (value.includes('A/C Name')) {
            const parts = value.split(':');
            if (parts.length > 1) {
              accountDetails['Account Name'] = parts[1].trim();
            }
          } else if (value.includes('BANK NAME')) {
            const parts = value.split(':');
            if (parts.length > 1) {
              accountDetails['Bank Name'] = parts[1].trim();
            }
          } else if (value.includes('BRANCH NAME')) {
            const parts = value.split(':');
            if (parts.length > 1) {
              accountDetails['Branch'] = parts[1].trim();
            }
          } else if (value.includes('IFSC Code')) {
            const parts = value.split(':');
            if (parts.length > 1) {
              accountDetails['IFSC Code'] = parts[1].trim();
            }
          }
        });
      }
    } catch (error) {
      console.error('Error extracting account info:', error);
      // Return empty object on error rather than throwing
    }
    return accountDetails;
  };

  const handlePDFUpload = async (file: File) => {
    setLoading(true)
    setError(null)
    setTableData([])
    setExcelBlob(null)
    setAccountInfo({})
    setProcessingStatus('Uploading PDF file...')

    try {
      const formData = new FormData()
      formData.append('file', file)

      setProcessingStatus('Extracting tables from PDF...')
      const response = await fetch('http://localhost:8080/api/extract-table', {
        method: 'POST',
        body: formData,
      })

      let responseData;
      const responseText = await response.text()
      
      try {
        responseData = JSON.parse(responseText)
      } catch (parseError) {
        console.error('Error parsing response:', parseError, 'Response text:', responseText)
        throw new Error('Invalid JSON response from server')
      }

      if (!response.ok) {
        throw new Error(responseData.error || responseData.message || 'Failed to process PDF')
      }

      // Validate response data structure
      if (!Array.isArray(responseData)) {
        throw new Error('Invalid data format: expected an array of tables')
      }

      // Validate each table in the response
      responseData.forEach((table, index) => {
        if (!table.headers || !Array.isArray(table.headers)) {
          throw new Error(`Invalid table format at index ${index}: missing headers array`)
        }
        if (!table.rows || !Array.isArray(table.rows)) {
          throw new Error(`Invalid table format at index ${index}: missing rows array`)
        }
        if (typeof table.pageNumber !== 'number') {
          throw new Error(`Invalid table format at index ${index}: missing page number`)
        }
      })

      setTableData(responseData)
      setAccountInfo(extractAccountInfo(responseData))
      
      // Then, get Excel file
      setProcessingStatus('Generating Excel file...')
      const excelResponse = await fetch('http://localhost:8080/api/extract-and-export', {
        method: 'POST',
        body: formData,
      })

      if (!excelResponse.ok) {
        const excelErrorText = await excelResponse.text()
        let excelError
        try {
          excelError = JSON.parse(excelErrorText)
          throw new Error(excelError.error || excelError.message || 'Failed to generate Excel file')
        } catch (parseError) {
          console.error('Error parsing Excel error response:', parseError, 'Response text:', excelErrorText)
          throw new Error('Failed to generate Excel file')
        }
      }

      const excelData = await excelResponse.blob()
      if (!excelData || excelData.size === 0) {
        throw new Error('Generated Excel file is empty')
      }
      
      setExcelBlob(excelData)
      setProcessingStatus('Processing complete!')
    } catch (error) {
      console.error('Error uploading PDF:', error)
      setError(error instanceof Error ? error.message : 'Failed to process PDF')
      setTableData([])
      setExcelBlob(null)
      setAccountInfo({})
    } finally {
      setLoading(false)
    }
  }

  const handleDownloadExcel = () => {
    if (excelBlob) {
      const url = window.URL.createObjectURL(excelBlob)
      const a = document.createElement('a')
      a.href = url
      a.download = 'bank_statement.xlsx'
      document.body.appendChild(a)
      a.click()
      window.URL.revokeObjectURL(url)
      document.body.removeChild(a)
    }
  }

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <Container maxWidth="lg">
        <Box sx={{ my: 4 }}>
          <Typography variant="h3" component="h1" gutterBottom align="center">
            PDF Data Extractor
          </Typography>
          <Paper elevation={3} sx={{ p: 3, mb: 3 }}>
            <PDFUploader onUpload={handlePDFUpload} loading={loading} />
            {loading && (
              <Box sx={{ textAlign: 'center', mt: 2 }}>
                <CircularProgress />
                <Typography variant="body1" sx={{ mt: 1 }}>
                  {processingStatus}
                </Typography>
              </Box>
            )}
          </Paper>
          
          {Object.keys(accountInfo).length > 0 && (
            <Paper elevation={3} sx={{ p: 3, mb: 3 }}>
              <Typography variant="h5" gutterBottom>Account Information</Typography>
              <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', gap: 2 }}>
                {Object.entries(accountInfo).map(([key, value]) => (
                  <Box key={key}>
                    <Typography variant="subtitle2" color="textSecondary">{key}</Typography>
                    <Typography variant="body1">{value}</Typography>
                  </Box>
                ))}
              </Box>
            </Paper>
          )}
          
          {tableData.length > 0 && (
            <Paper elevation={3} sx={{ p: 3 }}>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                <Typography variant="h5">Statement Details</Typography>
                {excelBlob && (
                  <Button
                    variant="contained"
                    startIcon={<DownloadIcon />}
                    onClick={handleDownloadExcel}
                  >
                    Download Excel
                  </Button>
                )}
              </Box>
              <Divider sx={{ mb: 2 }} />
              <TableDisplay data={tableData} />
            </Paper>
          )}

          <Snackbar 
            open={!!error} 
            autoHideDuration={6000} 
            onClose={() => setError(null)}
            anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
          >
            <Alert 
              onClose={() => setError(null)} 
              severity="error" 
              sx={{ width: '100%' }}
            >
              {error}
            </Alert>
          </Snackbar>
        </Box>
      </Container>
    </ThemeProvider>
  )
}

export default App
