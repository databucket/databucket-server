import MaterialTable from "material-table";
import React, {useContext, useEffect, useState} from "react";
import FilterList from "@mui/icons-material/FilterList";
import {useTheme} from "@mui/material/styles";
import {
    getTableHeaderBackgroundColor, getTableRowBackgroundColor, getTemplatePageSizeOptions, getTemplateTableHeight
} from "../../../utils/MaterialTableHelper";
import {isItemChanged, validateItem} from "../../../utils/JsonHelper";
import {MessageBox} from "../../utils/MessageBox";
import {getColumnBuckets, getColumnClasses, getColumnCreatedAt, getColumnDescription, getColumnModifiedAt, getColumnModifiedBy, getColumnName} from "../../utils/StandardColumns";
import {useWindowDimension} from "../../utils/UseWindowDimension";
import TemplatesContext from "../../../context/templates/TemplatesContext";
import PropTypes from "prop-types";
import {getTemplatesArtefacts, getTemplatesArtefactsEditable, templateArtefactCreationEnrichment, templateArtefactModifyingEnrichment} from "./_TemplUtils";

TemplConfigTagsTab.propTypes = {
    template: PropTypes.object.isRequired,
    setTemplate: PropTypes.func.isRequired
}

export default function TemplConfigTagsTab(props) {

    const theme = useTheme();
    const [height] = useWindowDimension();
    const tableRef = React.createRef();
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    const [filtering, setFiltering] = useState(false);
    const templatesContext = useContext(TemplatesContext);
    const {templates} = templatesContext;
    const [tags, setTags] = useState(getTemplatesArtefactsEditable(props.template, templates, 'tags'));
    const [buckets, setBuckets] = useState([]);
    const [classes, setClasses] = useState([]);
    const changeableFields = ['name', 'description', 'bucketsIds', 'classesIds'];
    const fieldsSpecification = {
        name: {title: 'Name', check: ['notEmpty', 'min1', 'max30']},
        description: {title: 'Description', check: ['max250']}
    };

    useEffect(() => {
        setBuckets(getTemplatesArtefacts(props.template, templates, 'buckets'));
    }, [templates]);

    useEffect(() => {
        setClasses(getTemplatesArtefacts(props.template, templates, 'classes'));
    }, [templates]);

    const onConfigurationChanged = (newTags) => {
        setTags(newTags);
        const template = props.template;
        template['configuration']['tags'] = newTags.filter(item => !(item.editable === false));
        props.setTemplate(template);
    }

    return (
        <div>
            <MaterialTable
                title='Tags'
                tableRef={tableRef}
                columns={[
                    getColumnName(),
                    getColumnDescription(),
                    getColumnBuckets(buckets, 'Available for buckets'),
                    getColumnClasses(classes, 'Available by classes'),
                    {...getColumnCreatedAt(), defaultSort: 'asc', hidden: true},
                    getColumnModifiedBy(),
                    getColumnModifiedAt()
                ]}
                data={tags}
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
                        icon: () => <FilterList/>,
                        tooltip: 'Enable/disable filter',
                        isFreeAction: true,
                        onClick: () => setFiltering(!filtering)
                    }
                ]}
                editable={{
                    isEditable: rowData => rowData.editable !== false,
                    isDeletable: rowData => rowData.editable !== false,
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

                            templateArtefactCreationEnrichment(newData);
                            onConfigurationChanged([...tags, newData]);
                            resolve();
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

                            templateArtefactModifyingEnrichment(newData);
                            onConfigurationChanged([...tags.filter(t => t.id !== oldData.id), newData]);
                            resolve();
                        }),

                    onRowDelete: oldData =>
                        new Promise((resolve, reject) => {
                            onConfigurationChanged([...tags.filter(t => t.id !== oldData.id)]);
                            resolve();
                        }),
                }}
            />
            <MessageBox
                config={messageBox}
                onClose={() => setMessageBox({...messageBox, open: false})}
            />
        </div>
    );
}