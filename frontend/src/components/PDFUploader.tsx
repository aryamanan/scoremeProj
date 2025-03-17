import { useCallback } from 'react';
import { useDropzone } from 'react-dropzone';
import { Box, Typography, CircularProgress } from '@mui/material';
import CloudUploadIcon from '@mui/icons-material/CloudUpload';

interface PDFUploaderProps {
  onUpload: (file: File) => void;
  loading: boolean;
}

const PDFUploader = ({ onUpload, loading }: PDFUploaderProps) => {
  const onDrop = useCallback((acceptedFiles: File[]) => {
    if (acceptedFiles.length > 0) {
      onUpload(acceptedFiles[0]);
    }
  }, [onUpload]);

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: {
      'application/pdf': ['.pdf']
    },
    multiple: false
  });

  return (
    <Box
      {...getRootProps()}
      sx={{
        border: '2px dashed',
        borderColor: isDragActive ? 'primary.main' : 'grey.300',
        borderRadius: 2,
        p: 3,
        textAlign: 'center',
        cursor: 'pointer',
        bgcolor: isDragActive ? 'action.hover' : 'background.paper',
        '&:hover': {
          bgcolor: 'action.hover',
        },
      }}
    >
      <input {...getInputProps()} />
      {loading ? (
        <CircularProgress />
      ) : (
        <>
          <CloudUploadIcon sx={{ fontSize: 48, color: 'primary.main', mb: 2 }} />
          <Typography variant="h6" gutterBottom>
            {isDragActive ? 'Drop the PDF here' : 'Drag & drop a PDF file here'}
          </Typography>
          <Typography variant="body2" color="textSecondary">
            or click to select a file
          </Typography>
        </>
      )}
    </Box>
  );
};

export default PDFUploader; 