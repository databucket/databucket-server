import React, {useContext, useEffect, useRef, useState} from 'react';
import PropTypes from 'prop-types';
import MaterialTable from '@material-table/core';
import {
    Box,
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    MenuItem,
    Tab,
    Tabs,
    TextField,
    Tooltip,
    useTheme
} from '@mui/material';
import {InfoOutlined, MoreHoriz, WarningAmber} from '@mui/icons-material';
import {alpha} from '@mui/material/styles';
import EnumsContext from "../../context/enums/EnumsContext";
import PropertiesTable from "../utils/PropertiesTable";
import {MessageBox} from "../utils/MessageBox";
import {
    getTableBodyHeight,
    getTableHeaderBackgroundColor,
    getTableRowBackgroundColor,
    withoutHeaderSorting
} from "../../utils/MaterialTableHelper";
import {uuidV4} from "../../utils/JsonHelper";

export const MAX_INDEXED_PROPERTIES = 5;
// the same as IndexedPropertyDto.PATH_PATTERN on the server
const PATH_PATTERN = /^\$(\.[\p{L}\p{N}_\- ]+)+$/u;

// an operator of the rules in the info text
const Op = ({children}) => (
    <Box component='code' sx={{fontFamily: 'monospace', fontWeight: 'bold', color: 'secondary.light'}}>{children}</Box>
);

const INFO = (
    <span>
        A database index for each property makes the rules on it fast in a big bucket: text
        for <Op>==</Op>, <Op>in</Op> and <Op>like</Op> with a fixed beginning (e.g. A%), number
        for <Op>==</Op>, <Op>&lt;</Op>, <Op>&gt;</Op> and <Op>between</Op>.
    </span>
);

EditIndexedPropertiesDialog.propTypes = {
    rowData: PropTypes.object.isRequired,
    classes: PropTypes.array,
    onChange: PropTypes.func.isRequired
}

const indexType = property => property != null && property.type === 'numeric' ? 'number' : 'text';

// the names of the property types, as in the Properties tab
const PROPERTY_TYPES = {
    string: 'String', numeric: 'Numeric', datetime: 'Datetime', date: 'Date', time: 'Time', boolean: 'Boolean',
    select: 'Enum'
};

/**
 * The indexed properties of a bucket: a database index for each, so that rules on them are fast in a big bucket.
 * Like the rich filter: the indexes are chosen from the properties (of the class and defined here), the Properties tab
 * defines more. Only the indexes are saved, a defined property stays as long as it is indexed.
 */
export default function EditIndexedPropertiesDialog(props) {
    const theme = useTheme();
    const [open, setOpen] = useState(false);
    const [activeTab, setActiveTab] = useState(0);
    const [indexes, setIndexes] = useState([]);
    const [properties, setProperties] = useState([]);
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    const contentRef = useRef(null);
    const {enums, fetchEnums} = useContext(EnumsContext);
    const current = props.rowData.indexedProperties || [];

    useEffect(() => {
        if (open && enums == null)
            fetchEnums();
    }, [open, enums, fetchEnums]);

    const getClassProperties = () => {
        const dataClass = props.classes != null ? props.classes.find(c => String(c.id) === String(props.rowData.classId)) : null;
        return dataClass != null && dataClass.configuration != null ? dataClass.configuration : [];
    }

    const handleOpen = () => {
        const classProperties = getClassProperties();
        // an indexed path that is not a property of the class comes back as a defined property
        const defined = current
            .filter(index => !classProperties.some(p => p.path === index.path))
            .map(index => ({uuid: uuidV4(), title: index.path.substring(2), path: index.path,
                type: index.type === 'number' ? 'numeric' : 'string'}));
        setProperties([...classProperties, ...defined]);
        setIndexes(current.map(index => ({...index})));
        setActiveTab(0);
        setOpen(true);
    }

    const handleSave = () => {
        props.onChange(indexes.map(index => ({path: index.path, type: index.type})));
        setOpen(false);
    }

    const showError = (title, message) => setMessageBox({open: true, severity: 'warning', title, message});

    const indexable = properties.filter(p => p.path != null && PATH_PATTERN.test(p.path));
    const propertyTitle = path => {
        const property = properties.find(p => p.path === path);
        return property != null && property.title ? property.title : '';
    }

    // the type of the index of the path as its property says, null when the property isn't there
    const expectedType = path => {
        const property = properties.find(p => p.path === path);
        return property != null ? indexType(property) : null;
    }

    // why the type of the index doesn't match its property, null when it does
    const typeMismatch = index => {
        const expected = expectedType(index.path);
        if (expected == null || expected === index.type)
            return null;
        const property = properties.find(p => p.path === index.path);
        return `The property is ${PROPERTY_TYPES[property.type] || property.type}: the index should be ${expected}.`
            + ' Edit the row and save it to change the type.';
    }

    // the error of an index, null when valid
    const validate = (index, others) => {
        if (index.path == null || index.path === '')
            return 'Choose a property.';
        if (!PATH_PATTERN.test(index.path))
            return 'A property like $.a.b (letters, digits, _, - and spaces).';
        if (others.some(other => other.path === index.path))
            return 'The property is already indexed.';
        return null;
    }

    const bodyHeight = () => getTableBodyHeight(contentRef, 66);

    return (
        <div>
            <Tooltip title={'Edit indexed properties'}>
                <Button endIcon={<MoreHoriz/>} onClick={handleOpen} style={{textTransform: 'none'}} color={'inherit'}>
                    {`${current.length}`}
                </Button>
            </Tooltip>
            <Dialog open={open} onClose={() => setOpen(false)} fullWidth maxWidth='md'>
                <DialogTitle sx={{display: 'flex', alignItems: 'center'}}>
                    <Box sx={{flex: 1}}>Indexed properties</Box>
                    <Tabs value={activeTab} onChange={(e, tab) => setActiveTab(tab)}
                          textColor="secondary" indicatorColor="secondary">
                        <Tab label="Indexes"/>
                        <Tab label="Properties"/>
                    </Tabs>
                    <Box sx={{flex: 1}}/>
                </DialogTitle>
                <DialogContent dividers ref={contentRef} sx={{height: '42vh', p: 1, overflow: 'hidden'}}>
                    {open && activeTab === 0 &&
                        <MaterialTable
                            title={`Indexes (${indexes.length} of ${MAX_INDEXED_PROPERTIES}):`}
                            columns={withoutHeaderSorting([
                                {title: '#', cellStyle: {width: '1%'}, editable: 'never',
                                    render: rowData => rowData ? indexes.findIndex(index => index.path === rowData.path) + 1 || '' : ''},
                                {
                                    title: 'Property', field: 'title',
                                    editComponent: editProps => (
                                        <TextField select size='small' fullWidth variant='standard'
                                                   value={editProps.rowData.path || ''}
                                                   onChange={e => {
                                                       const path = e.target.value;
                                                       editProps.onRowDataChange({...editProps.rowData, path, title: propertyTitle(path),
                                                           type: indexType(properties.find(p => p.path === path))});
                                                   }}>
                                            {indexable
                                                .filter(p => p.path === editProps.rowData.path || !indexes.some(index => index.path === p.path))
                                                .map(p => (
                                                <MenuItem key={p.path} value={p.path}>{p.title || p.path}</MenuItem>
                                            ))}
                                        </TextField>
                                    )
                                },
                                // set by the chosen property
                                {title: 'Path', field: 'path', editable: 'never'},
                                {title: 'Type', field: 'type', editable: 'never',
                                    render: rowData => {
                                        const mismatch = typeMismatch(rowData);
                                        return mismatch == null ? rowData.type : (
                                            <Tooltip title={mismatch}>
                                                <Box component='span' sx={{display: 'inline-flex', alignItems: 'center', gap: 0.5}}>
                                                    {rowData.type}<WarningAmber fontSize='small' color='warning'/>
                                                </Box>
                                            </Tooltip>
                                        );
                                    }}
                            ])}
                            data={indexes.map(index => ({...index, title: propertyTitle(index.path)}))}
                            options={{
                                paging: false,
                                search: false,
                                actionsColumnIndex: -1,
                                padding: 'dense',
                                headerStyle: {
                                    position: 'sticky',
                                    top: 0,
                                    backgroundColor: getTableHeaderBackgroundColor(theme),
                                    zIndex: 2
                                },
                                minBodyHeight: bodyHeight(),
                                maxBodyHeight: bodyHeight(),
                                rowStyle: rowData => ({
                                    backgroundColor: typeMismatch(rowData) != null
                                        ? alpha(theme.palette.warning.main, 0.15)
                                        : getTableRowBackgroundColor(rowData, theme)
                                })
                            }}
                            components={{Container: containerProps => <div {...containerProps}/>}}
                            editable={{
                                onRowAdd: indexes.length >= MAX_INDEXED_PROPERTIES ? undefined : newData =>
                                    new Promise((resolve, reject) => {
                                        const error = validate(newData, indexes);
                                        if (error != null) {
                                            showError('Index is not valid', error);
                                            reject();
                                            return;
                                        }
                                        setIndexes([...indexes, {path: newData.path, type: expectedType(newData.path) || newData.type}]);
                                        resolve();
                                    }),
                                onRowUpdate: (newData, oldData) =>
                                    new Promise((resolve, reject) => {
                                        const error = validate(newData, indexes.filter(index => index.path !== oldData.path));
                                        if (error != null) {
                                            showError('Index is not valid', error);
                                            reject();
                                            return;
                                        }
                                        setIndexes(indexes.map(index => index.path === oldData.path
                                            ? {path: newData.path, type: expectedType(newData.path) || newData.type} : index));
                                        resolve();
                                    }),
                                onRowDelete: oldData =>
                                    new Promise(resolve => {
                                        setIndexes(indexes.filter(index => index.path !== oldData.path));
                                        resolve();
                                    })
                            }}
                        />}
                    {open && activeTab === 1 &&
                        <PropertiesTable
                            used={properties.filter(p => indexes.some(index => index.path === p.path)).map(p => p.uuid)}
                            data={properties}
                            enums={enums != null ? enums : []}
                            onChange={setProperties}
                            title={'Class origin and defined properties:'}
                            pageSize={null}
                            parentContentRef={contentRef}
                        />}
                </DialogContent>
                <DialogActions>
                    <Tooltip title={INFO} slotProps={{tooltip: {sx: {fontSize: '0.875rem', maxWidth: 420}}}}>
                        <InfoOutlined color='action' sx={{ml: 1}}/>
                    </Tooltip>
                    <Box sx={{flex: 1}}/>
                    <Button onClick={() => setOpen(false)}>Cancel</Button>
                    <Button onClick={handleSave} variant='contained'>OK</Button>
                </DialogActions>
            </Dialog>
            <MessageBox config={messageBox} onClose={() => setMessageBox({...messageBox, open: false})}/>
        </div>
    );
}
