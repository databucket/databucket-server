import MaterialTable from "material-table";
import React, {useContext, useEffect, useRef, useState} from "react";
import Refresh from "@mui/icons-material/Refresh";
import FilterList from "@mui/icons-material/FilterList";
import {useTheme} from "@mui/material/styles";
import {getLastPageSize, setLastPageSize} from "../../utils/ConfigurationStorage";
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
import {useWindowDimension} from "../utils/UseWindowDimension";
import {getBaseUrl} from "../../utils/UrlBuilder";
import SvgContext from "../../context/svgs/SvgContext";
import IconButton from "@mui/material/IconButton";
import parse from "html-react-parser";
import {parseCustomSvg} from "../utils/SvgHelper";

export default function SvgTab() {

    const theme = useTheme();
    const inputRef = useRef(null);
    const [height] = useWindowDimension();
    const tableRef = React.createRef();
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    const [pageSize, setPageSize] = useState(getLastPageSize);
    const [filtering, setFiltering] = useState(false);
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

    const onChangeRowsPerPage = (pageSize) => {
        setPageSize(pageSize);
        setLastPageSize(pageSize);
    }

    const onLoadSvgFromFile = () => {
        inputRef.current.click();
    }

    const onFileInputChange = () => {
        if (window.File && window.FileReader && window.FileList && window.Blob) {
            const file = document.querySelector('input[type=file]').files[0];
            const reader = new FileReader();
            const svgFile = /svg.*/;

            if (file.type.match(svgFile)) {
                reader.onload = function (event) {
                    insertSvg(file.name, event.target.result);
                }
            } else
                setMessageBox({
                    open: true,
                    severity: 'error',
                    title: 'Error',
                    message: "It doesn't seem to be an svg file!"
                });

            reader.readAsText(file);
        }
    }

    const insertSvg = (name, svgContent) => {
        const size = ['24', '24px'];
        const svgObj = parse(svgContent.trim());
        if (svgObj.type !== "svg") {
            setMessageBox({
                open: true,
                severity: 'error',
                title: 'Error',
                message: "It seems, the file content isn't an svg object!"
            });
            return;
        }
        if ((svgObj.props.height != null && !size.includes(svgObj.props.height))
            || (svgObj.props.width != null && !size.includes(svgObj.props.width))
            || (svgObj.props.viewBox != null && svgObj.props.viewBox !== "0 0 24 24")) {
            setMessageBox({
                open: true,
                severity: 'error',
                title: 'Error',
                message: "The SVG object is expected to be configured with viewBox=\"0 0 24 24\" height=\"24px\" width=\"24px\"!"
            });
            return;
        }

        const childrenContent = svgContent.substring(svgContent.indexOf(">") + 1).replace("</svg>", "");
        const payload = {name: name, structure: childrenContent};
        fetch(getBaseUrl('svg'), getPostOptions(payload))
            .then(handleErrors)
            .catch(error => {
                setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
            })
            .then(() => {
                fetchSvgs();
            });
    }

    return (
        <div>
            <MaterialTable
                title='Svg icons'
                tableRef={tableRef}
                columns={[
                    {
                        title: 'Image',
                        sorting: false,
                        field: 'structure',
                        searchable: false,
                        filtering: false,
                        editable: "never",
                        render: rowData => <IconButton
                            size="large">{parseCustomSvg(rowData.structure, getButtonColor(theme))}</IconButton>,
                    },
                    getColumnName(),
                    getColumnCreatedBy(),
                    getColumnCreatedAt(),
                    getColumnModifiedBy(),
                    getColumnModifiedAt()
                ]}
                data={svgs != null ? svgs : []}
                onChangeRowsPerPage={onChangeRowsPerPage}
                options={{
                    pageSize: pageSize,
                    pageSizeOptions: getPageSizeOptions(),
                    paginationType: 'stepped',
                    actionsColumnIndex: -1,
                    sorting: true,
                    search: true,
                    filtering: filtering,
                    debounceInterval: 700,
                    padding: 'dense',
                    headerStyle: {backgroundColor: getTableHeaderBackgroundColor(theme)},
                    maxBodyHeight: getSettingsTableHeight(height),
                    minBodyHeight: getSettingsTableHeight(height),
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
                        onClick: () => setFiltering(!filtering)
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

                    onRowDelete: oldData =>
                        new Promise((resolve, reject) => {
                            setTimeout(() => {
                                let e = false;
                                fetch(getBaseUrl(`svg/${oldData.id}`), getDeleteOptions())
                                    .then(handleErrors)
                                    .catch(error => {
                                        e = true;
                                        if (error.includes('already used by items'))
                                            setMessageBox({
                                                open: true,
                                                severity: 'warning',
                                                title: 'Item can not be removed',
                                                message: error
                                            });
                                        else
                                            setMessageBox({
                                                open: true,
                                                severity: 'error',
                                                title: 'Error',
                                                message: error
                                            });
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
                accept="image/svg+xml"
                type="file"
                onChange={onFileInputChange}
                id="icon-button-file"
                style={{display: 'none',}}
            />
        </div>
    );
}
