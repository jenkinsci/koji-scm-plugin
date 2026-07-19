import React from "react"
import { useObserver } from "mobx-react"
import { FormControl, FormLabel, IconButton } from "@mui/material"
import { Add, } from "@mui/icons-material"
import DeleteButton from "../DeleteButton"

interface FormListProps {
    data: any[]
    label: string
    onAdd: () => any
    renderItem: (item: any, index: number) => React.ReactNode
}

const FormList: React.FC<FormListProps> = props => {
    return useObserver(() => {
        const { data, label, onAdd, renderItem } = props
        return (
            <div>
                <FormControl fullWidth margin="normal">
                    <FormLabel>{label}</FormLabel>
                    {data.map((item, index) => (
                        <div style={{ flexDirection: "row" }} key={index}>
                            {renderItem(item, index)}
                            <DeleteButton onClick={() => data.splice(index, 1)} />
                        </div>
                    ))}
                </FormControl>
                <IconButton onClick={() => data.push(onAdd())}>
                    <Add />
                </IconButton>
            </div>
        )
    })
}

export default FormList
