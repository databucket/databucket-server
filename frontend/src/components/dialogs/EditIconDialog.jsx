import React from 'react';
import PropTypes from 'prop-types';
import Dialog from '@material-ui/core/Dialog';
import Tooltip from '@material-ui/core/Tooltip';
import IconButton from '@material-ui/core/IconButton';
import DynamicIcon from '../DynamicIcon';

// const iconsNames = [
//     'Accessibility', 'Accessible', 'AccountBalance', 'AccountBalanceWallet', 'AccountBox', 'Alarm', 'AlarmOn', 'AllOut', 'Announcement', 'AspectRatio', 'Assessment', 'Assignment',
//     'AssignmentInd', 'AssignmentLate', 'AssignmentReturn', 'AssignmentReturned', 'AssignmentTurnedIn', 'Backup', 'Book', 'Bookmark', 'BugReport', 'Build', 'CalendarToday',
//     'CameraEnhance', 'CardGiftcard', 'CardMembership', 'CardTravel', 'ChangeHistory', 'CheckCircle', 'Commute', 'Copyright', 'CreditCard', 'Dashboard', 'DateRange', 'Description',
//     'Dns', 'DonutLarge', 'DonutSmall', 'DragIndicator', 'Eject', 'EuroSymbol', 'Event', 'EventSeat', 'ExitToApp', 'InvertColors', 'Label', 'Language', 'Loyalty', 'Motorcycle',
//     'OfflineBolt', 'OfflinePin', 'Opacity', 'OpenWith', 'Pageview', 'PanTool', 'Pets', 'PlayForWork', 'Polymer', 'Print', 'QueryBuilder', 'Receipt', 'Reorder', 'ReportProblem',
//     'Restore', 'RestoreFromTrash', 'RestorePage', 'Room', 'Search', 'SettingsApplications', 'SettingsBackupRestore', 'SettingsBluetooth', 'SettingsEthernet', 'SettingsInputComponent',
//     'SettingsPhone', 'SettingsVoice', 'Shop', 'ShoppingBasket', 'ShoppingCart', 'StarRate', 'Stars', 'Store', 'SupervisorAccount', 'Theaters', 'ThumbDown', 'ThumbUp', 'Timeline',
//     'TrendingDown', 'TrendingUp', 'TurnedIn', 'VerifiedUser', 'Visibility', 'WatchLater', 'Work', 'AddAlert', 'Error', 'NotificationImportant', 'Warning', 'Call', 'VpnKey', 'HowToReg',
//     'HowToVote', 'Report', 'Save', 'SaveAlt', 'Waves', 'Weekend', 'WhereToVote', 'FilterVintage', 'Grain', 'GridOn', 'Image', 'Lens', 'LooksOne', 'LooksTwo', 'Looks3', 'Looks4',
//     'Looks5', 'Looks6', 'MusicNote', 'PanoramaFishEye', 'Portrait', 'Style', 'Tune', 'WbSunny', 'Category', 'LocalOffer', 'LocalParking', 'LocalDrink', 'Subway', 'Train', 'Tram',
//     'Traffic', 'TripOrigin', 'Power', 'PriorityHigh', 'SdCard', 'Wifi', 'Wc', 'Casino', 'Pool', 'MeetingRoom', 'Group', 'Person', 'Share'
// ];

const iconsNames = [
    'AccessibilityNew',
    'AccountBalanceWallet',
    'AccountBox',
    'AccountCircle',
    'Alarm',
    'AlarmAdd',
    'AlarmOn',
    'AllInbox',
    'AllOut',
    'Android',
    'Announcement',
    'AspectRatio',
    'Assessment',
    'Assignment',
    'AssignmentTurnedIn',
    'Autorenew',
    'Backup',
    'Book',
    'Bookmark',
    'BookmarkBorder',
    'Bookmarks',
    'BugReport',
    'Build',
    'CardGiftcard',
    'CardMembership',
    'CardTravel',
    'ChangeHistory',
    'CheckCircle',
    'CheckCircleOutline',
    'ChromeReaderMode',
    'Code',
    'Commute',
    'CompareArrows',
    'ContactSupport',
    'Contactless',
    'Copyright',
    'CreditCard',
    'Dashboard',
    'DateRange',
    'Delete',
    'DeleteForever',
    'DeleteOutline',
    'Description',
    'Dns',
    'Done',
    'DoneAll',
    'DoneOutline',
    'DonutLarge',
    'DonutSmall',
    'DragIndicator',
    'Eco',
    'Eject',
    'EuroSymbol',
    'Event',
    'EventSeat',
    'ExitToApp',
    'Explore',
    'Extension',
    'Face',
    'Favorite',
    'FavoriteBorder',
    'Feedback',
    'FindInPage',
    'FindReplace',
    'Fingerprint',
    'FlipToBack',
    'FlipToFront',
    'GTranslate',
    'Gavel',
    'GetApp',
    'Gif',
    'Grade',
    'GroupWork',
    'Help',
    'HelpOutline',
    'HighlightOff',
    'History',
    'Home',
    'HourglassEmpty',
    'HourglassFull',
    'ImportantDevices',
    'Info',
    'Input',
    'InvertColors',
    'Label',
    'LabelImportant',
    'Language',
    'Launch',
    'List',
    'Lock',
    'LockOpen',
    'Loyalty',
    'Motorcycle',
    'NoteAdd',
    'OfflineBolt',
    'OfflinePin',
    'Opacity',
    'OpenInBrowser',
    'OpenWith',
    'Pageview',
    'PanTool',
    'Payment',
    'PermDataSetting',
    'PermDeviceInformation',
    'PowerSettingsNew',
    'Print',
    'Restore',
    'RestoreFromTrash',
    'RestorePage',
    'Room',
    'Schedule',
    'Search',
    'Settings',
    'SettingsApplications',
    'SettingsBackupRestore',
    'SettingsCell',
    'SettingsEthernet',
    'SettingsInputComponent',
    'SettingsPower',
    'SettingsRemote',
    'SettingsVoice',
    'Shop',
    'ShopTwo',
    'ShoppingBasket',
    'ShoppingCart',
    'Stars',
    'Store',
    'SwapHorizontalCircle',
    'SwapVerticalCircle',
    'ThumbDown',
    'ThumbUp',
    'ThumbsUpDown',
    'Timeline',
    'Today',
    'TouchApp',
    'Translate',
    'TrendingDown',
    'TrendingFlat',
    'TrendingUp',
    'TurnedIn',
    'TurnedInNot',
    'Update',
    'VerifiedUser',
    'ViewAgenda',
    'ViewArray',
    'ViewCarousel',
    'ViewColumn',
    'ViewHeadline',
    'ViewModule',
    'ViewStream',
    'Visibility',
    'VisibilityOff',
    'WatchLater',
    'Work',
    'WorkOutline',
    'YoutubeSearchedFor',
    'ZoomIn',
    'ZoomOut',
    'AddAlert',
    'Error',
    'ErrorOutline',
    'NotificationImportant',
    'Warning',
    'AddToQueue',
    'Airplay',
    'Album',
    'ArtTrack',
    'AvTimer',
    'ControlCamera',
    'Explicit',
    'FastForward',
    'FastRewind',
    'FiberNew',
    'FiberPin',
    'Games',
    'Mic',
    'MicNone',
    'MicOff',
    'Movie',
    'MusicVideo',
    'NotInterested',
    'Note',
    'PauseCircleOutline',
    'PlayCircleOutline',
    'Radio',
    'Repeat',
    'RepeatOne',
    'Shuffle',
    'Speed',
    'Chat',
    'LiveHelp',
    'MailOutline',
    'MobileScreenShare',
    'NoSim',
    'PhonelinkErase',
    'PhonelinkLock',
    'SpeakerPhone',
    'StayCurrentLandscape',
    'StayCurrentPortrait',
    'PhonelinkRing',
    'PhonelinkSetup',
    'Textsms',
    'Unsubscribe',
    'Voicemail',
    'VpnKey',
    'AddBox',
    'AddCircle',
    'AddCircleOutline',
    'Ballot',
    'Create',
    'DeleteSweep',
    'Drafts',
    'Flag',
    'OutlinedFlag',
    'FontDownload',
    'Forward',
    'Gesture',
    'HowToReg',
    'HowToVote',
    'Inbox',
    'Link',
    'Mail',
    'Policy',
    'RemoveCircle',
    'RemoveCircleOutline',
    'Save',
    'SaveAlt',
    'AddToHomeScreen',
    'DeveloperMode',
    'Devices',
    'GpsFixed',
    'GpsNotFixed',
    'MobileFriendly',
    'MobileOff',
    'ScreenLockPortrait',
    'SdStorage',
    'AttachFile',
    'AttachMoney',
    'BarChart',
    'BorderAll',
    'BubbleChart',
    'FormatBold',
    'InsertChart',
    'InsertEmoticon',
    'Cloud',
    'CloudDone',
    'CloudDownload',
    'Folder',
    'DeviceUnknown',
    'DevicesOther',
    'Dock',
    'Laptop',
    'PhoneAndroid',
    'PhoneIphone',
    'Security',
    'Smartphone',
    'Tablet',
    'Toys',
    'Tv',
    'Watch',
    'FlashOn',
    'LooksOne',
    'LooksTwo',
    'Looks3',
    'Looks4',
    'Looks5',
    'Looks6',
    'Photo',
    'Style',
    'WbSunny',
    'Category',
    'LocalOffer',
    'People',
    'Person',
    'Public',
    'Whatshot'
];

SimpleDialog.propTypes = {
    onClose: PropTypes.func,
    open: PropTypes.bool,
    selectedValue: PropTypes.string,
};

function SimpleDialog(props) {
    const { onClose, selectedValue, ...other } = props;

    function handleClose() {
        onClose(selectedValue);
    }

    function handleItemClick(value) {
        onClose(value);
    }

    return (
        <Dialog onClose={handleClose} aria-labelledby="simple-dialog-title" {...other}>
            <div color='inherit'>
                {iconsNames.map((iName, key) => (
                    <Tooltip title={iName} key={key}>
                        <IconButton
                            onClick={() => handleItemClick(iName)}
                            color="default"
                        >
                            <DynamicIcon iconName={iName}/>
                        </IconButton>
                    </Tooltip>
                ))}
            </div>
        </Dialog>
    );
}

SimpleDialog.propTypes = {
    onChange: PropTypes.func
};

export default function SelectIconDialog(props) {
    const { value, onChange } = props;
    const [open, setOpen] = React.useState(false);
    const [selectedValue, setSelectedValue] = React.useState(value);

    function handleClickOpen() {
        setOpen(true);
    }

    const handleClose = value => {
        setOpen(false);
        setSelectedValue(value);
        onChange(value);
    };

    return (
        <div>
            <Tooltip title={selectedValue != null ? selectedValue : 'undefined'}>
                <IconButton
                    onClick={handleClickOpen}
                    color="default"
                >
                    <DynamicIcon iconName={selectedValue} />
                </IconButton>
            </Tooltip>
            <SimpleDialog selectedValue={selectedValue} open={open} onClose={handleClose} />
        </div>
    );
}
