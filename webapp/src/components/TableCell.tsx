import React from "react"
import { TableCell as MUITableCell } from "@mui/material"

const TableCell: React.FC = ({ children }) => {
    return <MUITableCell padding="none">{children}</MUITableCell>
}

export default TableCell
