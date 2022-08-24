import moment from "moment";

export const getCurrentDateTimeStr = () => {
    const now = new Date(); //2022-03-17T05:22:17.927Z
    return moment(now).utc().format("YYYY-MM-DDTHH:mm:ss.SSS[Z]");
}