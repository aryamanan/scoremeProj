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
    const firstPage = data.find(page => page.pageNumber === 1);
    
    if (firstPage) {
      firstPage.rows.forEach(row => {
        const value = Object.values(row)[0];
        if (value.includes('Account No')) {
          accountDetails['Account Number'] = value.split(':')[1].trim();
        } else if (value.includes('A/C Name')) {
          accountDetails['Account Name'] = value.split(':')[1].trim();
        } else if (value.includes('BANK NAME')) {
          accountDetails['Bank Name'] = value.split(':')[1].trim();
        } else if (value.includes('BRANCH NAME')) {
          accountDetails['Branch'] = value.split(':')[1].trim();
        } else if (value.includes('IFSC Code')) {
          accountDetails['IFSC Code'] = value.split(':')[1].trim();
        }
      });
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

      const responseText = await response.text()
      if (!response.ok) {
        throw new Error(responseText || 'Failed to process PDF')
      }

      try {
        const data = JSON.parse(responseText)
        setTableData(data)
        setAccountInfo(extractAccountInfo(data))
        
        // Then, get Excel file
        setProcessingStatus('Generating Excel file...')
        const excelResponse = await fetch('http://localhost:8080/api/extract-and-export', {
          method: 'POST',
          body: formData,
        })

        if (!excelResponse.ok) {
          const excelError = await excelResponse.text()
          throw new Error(excelError || 'Failed to generate Excel file')
        }

        const excelData = await excelResponse.blob()
        setExcelBlob(excelData)
        setProcessingStatus('Processing complete!')
      } catch (parseError) {
        console.error('Error parsing response:', parseError)
        throw new Error('Invalid response from server')
      }
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
