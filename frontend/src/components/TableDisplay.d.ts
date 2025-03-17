import { FC } from 'react';

interface TableData {
  [key: string]: string | number;
}

interface TableDisplayProps {
  data: TableData[];
}

declare const TableDisplay: FC<TableDisplayProps>;
export default TableDisplay; 