import React from 'react';
import {
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
  Paper,
  Box
} from '@mui/material';

interface TableRow {
  [key: string]: string;
}

interface TablePage {
  headers: string[];
  rows: TableRow[];
  pageNumber: number;
}

interface TableDisplayProps {
  data: TablePage[];
}

const TableDisplay: React.FC<TableDisplayProps> = ({ data }) => {
  if (!data || data.length === 0) {
    return (
      <Typography variant="body1" color="textSecondary" align="center">
        No table data available
      </Typography>
    );
  }

  return (
    <Box sx={{ mb: 4 }}>
      {data.map((page, pageIndex) => (
        <Box key={pageIndex} sx={{ mb: 4 }}>
          <Typography variant="h6" gutterBottom>
            Page {page.pageNumber}
          </Typography>
          <TableContainer component={Paper}>
            <Table size="small">
              <TableHead>
                <TableRow>
                  {page.headers.map((header, index) => (
                    <TableCell key={index}>{header}</TableCell>
                  ))}
                </TableRow>
              </TableHead>
              <TableBody>
                {page.rows.map((row, rowIndex) => (
                  <TableRow key={rowIndex}>
                    {Object.values(row).map((value, cellIndex) => (
                      <TableCell key={cellIndex}>{value}</TableCell>
                    ))}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </Box>
      ))}
    </Box>
  );
};

export default TableDisplay; 