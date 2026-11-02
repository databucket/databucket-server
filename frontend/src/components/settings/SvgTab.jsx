import MaterialTable from '@material-table/core';
import React, {useContext, useEffect, useRef, useState} from "react";
import {getProjectTableKey} from "../../utils/ConfigurationStorage";
import useTableState from "../../utils/TableState";
import {selectCellText} from "../../utils/TableCellSelect";
import {FilterList, Refresh} from "@mui/icons-material";
import {IconButton, useTheme} from "@mui/material";
import {
    getButtonColor,
    getDeleteOptions,
    getPageSizeOptions,
    getPostOptions,
    getPutOptions,
    getSettingsTableHeight,
    getTableHeaderBackgroundColor,
    getTableRowBackgroundColor
} from "../../utils/MaterialTableHelper";
import {handleErrors} from "../../utils/FetchHelper";
import {isItemChanged, validateItem} from "../../utils/JsonHelper";
import {MessageBox} from "../utils/MessageBox";
import {
    getColumnCreatedAt,
    getColumnCreatedBy,
    getColumnModifiedAt,
    getColumnModifiedBy,
    getColumnName
} from "../utils/StandardColumns";
import {getBaseUrl} from "../../utils/UrlBuilder";
import SvgContext from "../../context/svgs/SvgContext";
import {parseCustomSvg, prepareSvgIcon} from "../utils/SvgHelper";

export default function SvgTab() {

    const theme = useTheme();
    const inputRef = useRef(null);
    const tableRef = React.createRef();
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    const tableState = useTableState(getProjectTableKey('settings-svg'));
    const svgContext = useContext(SvgContext);
    const {svgs, fetchSvgs, editSvg, removeSvg} = svgContext;
    const changeableFields = ['name', 'structure'];
    const fieldsSpecification = {
        name: {title: 'Name', check: ['notEmpty', 'min3', 'max200']},
        structure: {title: 'Svg content', check: ['notEmpty']}
    };

    useEffect(() => {
        if (svgs == null)
            fetchSvgs();
    }, [svgs, fetchSvgs]);
    const onLoadSvgFromFile = () => {
        inputRef.current.click();
    }

    // any number of SVG files of any size; each becomes an icon named after its file
    const onFileInputChange = async (event) => {
        const files = [...event.target.files];
        // the same file can be chosen again
        event.target.value = '';

        const failed = [];
        let added = 0;
        for (const file of files) {
            try {
                const structure = prepareSvgIcon(await file.text());
                const name = file.name.replace(/\.svg$/i, '');
                await fetch(getBaseUrl('svg'), getPostOptions({name, structure})).then(handleErrors);
                added++;
            } catch (error) {
                failed.push(`${file.name}: ${typeof error === 'string' ? error : error.message}`);
            }
        }

        if (added > 0)
            fetchSvgs();
        if (failed.length > 0)
            setMessageBox({
                open: true,
                severity: added > 0 ? 'warning' : 'error',
                title: added > 0 ? `${added} of ${files.length} icons added` : 'No icon added',
                message: failed.join('\n')
            });
    }

    return (
        <div onDoubleClick={selectCellText}>
            <MaterialTable
                title='Svg icons'
                tableRef={tableRef}
                columns={tableState.columns([
                    {
                        title: 'Image',
                        sorting: false,
                        field: 'structure',
                        searchable: false,
                        filtering: false,
                        editable: "never",
                        render: rowData => <IconButton size="small">{parseCustomSvg(rowData.structure, getButtonColor(theme))}</IconButton>,
                    },
                    getColumnName(),
                    getColumnCreatedBy(),
                    getColumnCreatedAt(),
                    getColumnModifiedBy(),
                    getColumnModifiedAt()
                ])}
                data={tableState.data(svgs != null ? svgs : [])}
                key={tableState.key}
                {...tableState.callbacks}
                options={{
                    ...tableState.options,
                    pageSizeOptions: getPageSizeOptions(),
                    paginationType: 'stepped',
                    actionsColumnIndex: -1,
                    maxColumnSort: 1,
                    search: true,
                    filtering: tableState.filtering,
                    debounceInterval: 700,
                    padding: 'dense',
                    headerStyle: {
                        position: 'sticky',
                        top: 0,
                        backgroundColor: getTableHeaderBackgroundColor(theme)
                    },
                    cellStyle: {whiteSpace: 'nowrap'},
                    maxBodyHeight: getSettingsTableHeight(),
                    minBodyHeight: getSettingsTableHeight(),
                    rowStyle: rowData => ({backgroundColor: getTableRowBackgroundColor(rowData, theme)})
                }}
                components={{
                    Container: props => <div {...props} />
                }}
                actions={[
                    {
                        icon: () => <Refresh/>,
                        tooltip: 'Refresh',
                        isFreeAction: true,
                        onClick: () => fetchSvgs()
                    },
                    {
                        icon: () => <FilterList/>,
                        tooltip: 'Enable/disable filter',
                        isFreeAction: true,
                        onClick: tableState.toggleFiltering
                    },
                    {
                        icon: () => <span className="material-icons">file_open</span>,
                        tooltip: 'Load from file',
                        isFreeAction: true,
                        onClick: () => onLoadSvgFromFile()
                    }
                ]}
                editable={{
                    // onRowAdd: newData =>
                    //     new Promise((resolve, reject) => {
                    //         let message = validateItem(newData, fieldsSpecification);
                    //         if (message != null) {
                    //             setMessageBox({
                    //                 open: true,
                    //                 severity: 'warning',
                    //                 title: 'Item is not valid',
                    //                 message: message
                    //             });
                    //             reject();
                    //             return;
                    //         }
                    //
                    //         fetch(getBaseUrl('svg'), getPostOptions(newData))
                    //             .then(handleErrors)
                    //             .catch(error => {
                    //                 setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                    //             })
                    //             .then((svgItem) => {
                    //                 if (svgItem != null) {
                    //                     addSvg(svgItem);
                    //                     resolve();
                    //                 }
                    //             });
                    //     }),

                    onRowUpdate: (newData, oldData) =>
                        new Promise((resolve, reject) => {

                            if (!isItemChanged(oldData, newData, changeableFields)) {
                                setMessageBox({
                                    open: true,
                                    severity: 'info',
                                    title: 'Nothing changed',
                                    message: ''
                                });
                                reject();
                                return;
                            }

                            let message = validateItem(newData, fieldsSpecification);
                            if (message != null) {
                                setMessageBox({
                                    open: true,
                                    severity: 'error',
                                    title: 'Item is not valid',
                                    message: message
                                });
                                reject();
                                return;
                            }

                            fetch(getBaseUrl('svg'), getPutOptions(newData))
                                .then(handleErrors)
                                .catch(error => {
                                    setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                                    reject();
                                })
                                .then((svgItem) => {
                                    if (svgItem != null) {
                                        editSvg(svgItem);
                                        resolve();
                                    }
                                });
                        }),

                    // objects that use the icon keep their copy of it
                    onRowDelete: oldData =>
                        new Promise((resolve, reject) => {
                            setTimeout(() => {
                                let e = false;
                                fetch(getBaseUrl(`svg/${oldData.id}`), getDeleteOptions())
                                    .then(handleErrors)
                                    .catch(error => {
                                        e = true;
                                        setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                                        reject();
                                    })
                                    .then(() => {
                                        if (!e) {
                                            removeSvg(oldData.id);
                                            resolve();
                                        }
                                    });

                            }, 100);
                        }),
                }}
            />
            <MessageBox
                config={messageBox}
                onClose={() => setMessageBox({...messageBox, open: false})}
            />
            <input
                ref={inputRef}
                accept="image/svg+xml,.svg"
                multiple
                type="file"
                onChange={onFileInputChange}
                id="icon-button-file"
                style={{display: 'none',}}
            />
        </div>
    );
}
