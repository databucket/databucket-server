import MaterialTable from "material-table";
import React, {useContext, useEffect, useState} from "react";
import FilterList from "@mui/icons-material/FilterList";
import {useTheme} from "@mui/material/styles";
import {
    getDeleteOptions,
    getPostOptions, getPutOptions,
    getTableHeaderBackgroundColor, getTableRowBackgroundColor, getTemplatePageSizeOptions, getTemplateTableHeight
} from "../../../utils/MaterialTableHelper";
import {convertNullValuesInObject, getSelectedValues, isItemChanged, validateItem} from "../../../utils/JsonHelper";
import {MessageBox} from "../../utils/MessageBox";
import {getColumnDescription, getColumnId, getColumnModifiedAt, getColumnModifiedBy, getColumnName} from "../../utils/StandardColumns";
import {useWindowDimension} from "../../utils/UseWindowDimension";
import PropTypes from "prop-types";
import {getBaseUrl} from "../../../utils/UrlBuilder";
import {handleErrors} from "../../../utils/FetchHelper";
import {getManageDataMapper} from "../../../utils/NullValueMappers";
import Refresh from "@mui/icons-material/Refresh";
import Grid from "@mui/material/Grid";
import Divider from "@mui/material/Divider";
import DataContext from "../../../context/templatesData/DataContext";
import DataItemsContext from "../../../context/templatesDataItems/DataItemsContext";

TemplConfigDataTab.propTypes = {
    template: PropTypes.object.isRequired,
    setTemplate: PropTypes.func.isRequired
}

export default function TemplConfigDataTab(props) {

    const theme = useTheme();
    const [height] = useWindowDimension();
    const tableRef = React.createRef();
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    const [filtering, setFiltering] = useState(false);
    const dataContext = useContext(DataContext);
    const {data, templateId, fetchData, addData, editData, removeData} = dataContext;
    const dataItemsContext = useContext(DataItemsContext);
    const {dataItems} = dataItemsContext;
    const changeableFields = ['id', 'name', 'description'];
    const fieldsSpecification = {
        name: {title: 'Name', check: ['notEmpty', 'min1', 'max30']},
        description: {title: 'Description', check: ['max250']}
    };

    useEffect(() => {
        if (data == null || templateId !== props.template.id)
            fetchData(props.template.id);
    }, [data, props.template.id]);

    return (
        <div>
            <Grid container spacing={0}>
                <Grid item xs={4} >
                    <MaterialTable
                        title='Data set'
                        tableRef={tableRef}
                        columns={[
                            getColumnName(),
                            getColumnDescription()
                        ]}
                        data={data != null ? data : []}
                        options={{
                            paging: false,
                            actionsColumnIndex: -1,
                            sorting: true,
                            search: false,
                            filtering: false,
                            debounceInterval: 700,
                            padding: 'dense',
                            headerStyle: {backgroundColor: getTableHeaderBackgroundColor(theme)},
                            maxBodyHeight: getTemplateTableHeight(height),
                            minBodyHeight: getTemplateTableHeight(height),
                            rowStyle: rowData => ({backgroundColor: getTableRowBackgroundColor(rowData, theme)})
                        }}
                        components={{
                            Container: props => <div {...props} />
                        }}
                        editable={{
                            onRowAdd: newData =>
                                new Promise((resolve, reject) => {
                                    let message = validateItem(newData, fieldsSpecification);
                                    if (message != null) {
                                        setMessageBox({
                                            open: true,
                                            severity: 'warning',
                                            title: 'Item is not valid',
                                            message: message
                                        });
                                        reject();
                                        return;
                                    }

                                    newData.templateId = props.template.id;

                                    fetch(getBaseUrl('templates/data'), getPostOptions(newData))
                                        .then(handleErrors)
                                        .catch(error => {
                                            reject();
                                            setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                                        })
                                        .then((data) => {
                                            if (data != null) {
                                                addData(convertNullValuesInObject(data, getManageDataMapper()));
                                                resolve();
                                            }
                                        });
                                }),

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

                                    const payload = getSelectedValues(newData, changeableFields);

                                    fetch(getBaseUrl('templates/data'), getPutOptions(payload))
                                        .then(handleErrors)
                                        .catch(error => {
                                            setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                                            reject();
                                        })
                                        .then((data) => {
                                            if (data != null) {
                                                editData(convertNullValuesInObject(data, getManageDataMapper()));
                                                resolve();
                                            }
                                        });
                                }),

                            onRowDelete: oldData =>
                                new Promise((resolve, reject) => {
                                    setTimeout(() => {
                                        let e = false;
                                        fetch(getBaseUrl(`templates/data/${oldData.id}`), getDeleteOptions())
                                            .then(handleErrors)
                                            .catch(error => {
                                                e = true;
                                                setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                                                reject();
                                            })
                                            .then(() => {
                                                if (!e) {
                                                    removeData(oldData.id);
                                                    resolve();
                                                }
                                            });

                                    }, 100);
                                }),
                        }}
                    />
                </Grid>
                <Divider orientation="vertical" flexItem style={{marginRight:"-1px"}} />
                <Grid item xs={8} >
                    <div style={{marginLeft: '1px'}}>
                        <MaterialTable
                            title='Data items'
                            tableRef={tableRef}
                            columns={[
                                getColumnId(),
                                getColumnModifiedBy(),
                                getColumnModifiedAt()
                            ]}
                            data={dataItems != null ? dataItems : []}
                            options={{
                                pageSize: 15,
                                pageSizeOptions: getTemplatePageSizeOptions(),
                                paginationType: 'stepped',
                                actionsColumnIndex: -1,
                                sorting: true,
                                search: true,
                                filtering: filtering,
                                debounceInterval: 700,
                                padding: 'dense',
                                headerStyle: {backgroundColor: getTableHeaderBackgroundColor(theme)},
                                maxBodyHeight: getTemplateTableHeight(height),
                                minBodyHeight: getTemplateTableHeight(height),
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
                                    onClick: () => fetchData(props.template.id)
                                },
                                {
                                    icon: () => <FilterList/>,
                                    tooltip: 'Enable/disable filter',
                                    isFreeAction: true,
                                    onClick: () => setFiltering(!filtering)
                                }
                            ]}
                            editable={{
                                onRowAdd: newData =>
                                    new Promise((resolve, reject) => {
                                        let message = validateItem(newData, fieldsSpecification);
                                        if (message != null) {
                                            setMessageBox({
                                                open: true,
                                                severity: 'warning',
                                                title: 'Item is not valid',
                                                message: message
                                            });
                                            reject();
                                            return;
                                        }

                                        newData.templateId = props.template.id;

                                        fetch(getBaseUrl('templates/data'), getPostOptions(newData))
                                            .then(handleErrors)
                                            .catch(error => {
                                                reject();
                                                setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                                            })
                                            .then((data) => {
                                                if (data != null) {
                                                    addData(convertNullValuesInObject(data, getManageDataMapper()));
                                                    resolve();
                                                }
                                            });
                                    }),

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

                                        const payload = getSelectedValues(newData, changeableFields);

                                        fetch(getBaseUrl('templates/data'), getPutOptions(payload))
                                            .then(handleErrors)
                                            .catch(error => {
                                                setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                                                reject();
                                            })
                                            .then((data) => {
                                                if (data != null) {
                                                    editData(convertNullValuesInObject(data, getManageDataMapper()));
                                                    resolve();
                                                }
                                            });
                                    }),

                                onRowDelete: oldData =>
                                    new Promise((resolve, reject) => {
                                        setTimeout(() => {
                                            let e = false;
                                            fetch(getBaseUrl(`templates/data/${oldData.id}`), getDeleteOptions())
                                                .then(handleErrors)
                                                .catch(error => {
                                                    e = true;
                                                    setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                                                    reject();
                                                })
                                                .then(() => {
                                                    if (!e) {
                                                        removeData(oldData.id);
                                                        resolve();
                                                    }
                                                });

                                        }, 100);
                                    }),
                            }}
                        />
                    </div>
                </Grid>
            </Grid>
            <MessageBox
                config={messageBox}
                onClose={() => setMessageBox({...messageBox, open: false})}
            />
        </div>
    );
}