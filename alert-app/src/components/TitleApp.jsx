import React from "react";
import {Link} from "react-router-dom";
import './css/TitleApp.css';

function TitleApp ({link, message}) {
    return (
        <div>
            <h1 className="title">Harmony Tracker</h1>
            <p className="slogan">Embracing peace, one harmonious blunder at a time!</p>
            <div className="button-container">
                <Link to={link} className="fancy-button">{message}</Link>
            </div>
        </div>

    )
}
export default TitleApp;