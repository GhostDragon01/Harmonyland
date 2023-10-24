import React, {useEffect, useState} from 'react';
import styled from 'styled-components';
import {buildStyles, CircularProgressbar} from 'react-circular-progressbar';
import 'react-circular-progressbar/dist/styles.css';
import {v4 as uuidv4} from 'uuid';
import Avatar from 'react-avatar';
import TitleApp from "./TitleApp";

const AppContainer = styled.div`
  text-align: center;
  padding: 20px;
  font-family: 'Roboto', sans-serif;

`;

const Title = styled.h1`
  color: #ffffff;
  margin-bottom: 20px;
  text-align: left;
  margin-left: 95px;
  margin-top: 60px;
`;

const AlertListContainer = styled.div`
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 20px;
`;

const AlertCard = styled.div`
  border: 1px solid #8b8e94;
  border-radius: 8px;
  padding: 10px;
  background-color: #d6eaf9;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 5px 6px #000000;
  width: 500px;
  max-width: 100%;
  transform: translate3d(0, 0, 0);
  transition: transform 0.3s ease-in-out;
  &:hover {
    transform: translate3d(0, -5px, 0);
    background-color: #f8f8f8;
    box-shadow: 0 6px 12px rgba(0, 0, 0, 0.1);
  }
`;

const AvatarContainer = styled.div`
  margin-right: 10px;
`;

const PersonInfoContainer = styled.div`
  display: flex;
  flex-direction: column;
  align-items: flex-start;
`;

const PersonName = styled.h3`
  color: #333;
  margin-bottom: 5px;
  text-align: center;
  font-size: 20px;
`;

const PersonDetail = styled.p`
  margin-bottom: 5px;
`;

const ProgressContainer = styled.div`
  width: 70px;
  height: 70px;
`;

const generateRandomAvatarUrl = (person) => {
    const randomName = person ? person.name : '';
    const avatarUrl = `https://avatars.dicebear.com/api/${person.gender === 'male' ? 'male' : 'female'}/${randomName}.svg`;
    return avatarUrl;
};

const getProgressColor = (harmonyScore) => {
    if (harmonyScore >= 70) {
        return '#42e89c'; // Green
    } else if (harmonyScore >= 40) {
        return '#f5d042'; // Yellow
    } else {
        return '#e84a5f'; // Red
    }
};

const formatTime = (time) => {
    const dateTime = new Date(time);
    const year = dateTime.getFullYear();
    const month = String(dateTime.getMonth() + 1).padStart(2, '0');
    const day = String(dateTime.getDate()).padStart(2, '0');
    const hours = String(dateTime.getHours()).padStart(2, '0');
    const minutes = String(dateTime.getMinutes()).padStart(2, '0');
    return `${year}-${month}-${day} ${hours}:${minutes}`;
};

const AlertInterface = () => {
    const [alertMessages, setAlertMessages] = useState([]);

    const fetchAlertMessages = async () => {
        try {
            const response = await fetch('http://localhost:8080/alerts');
            const data = await response.json();
            console.log('Server Response:', data);
            const alertMessages = data.map((item) => JSON.parse(item.message));
            console.log('Alert messages:', alertMessages);

            setAlertMessages((prevAlerts) => {
                const existingIds = prevAlerts.map((alert) => alert.harmonyWatcher.id);
                const newAlerts = alertMessages.filter(
                    (alert) => !existingIds.includes(alert.harmonyWatcher.id)
                );
                const updatedAlerts = [...prevAlerts, ...newAlerts];
                localStorage.setItem('alertMessages', JSON.stringify(updatedAlerts));
                return updatedAlerts;
            });
        } catch (error) {
            console.error('Error fetching alert messages:', error);
        }
    };

    useEffect(() => {
        const storedAlerts = JSON.parse(localStorage.getItem('alertMessages'));
        if (storedAlerts && Array.isArray(storedAlerts)) {
            setAlertMessages(storedAlerts);
        } else {
            setAlertMessages([]);
        }

        fetchAlertMessages();
        const interval = setInterval(fetchAlertMessages, 5000);
        return () => clearInterval(interval);
    }, []);

    return (
        <AppContainer>
            <TitleApp message={"Show Analytics"} link={"./analytics"}/>
            <Title>Alert Messages</Title>
            <AlertListContainer>
                {alertMessages.map((alert) => (
                    <AlertCard key={uuidv4()}>
                        <AvatarContainer>
                            <Avatar
                                name={alert.person ? alert.person.name : 'Unknown'}
                                src={alert.person ? generateRandomAvatarUrl(alert.person) : ''}
                                round
                                size={40}
                            />
                        </AvatarContainer>
                        <PersonInfoContainer>
                            <PersonName>{alert.person ? alert.person.name : 'Unknown'}</PersonName>
                            {alert.person ? (
                                <>
                                    <PersonDetail>
                                        <strong>Drone ID:</strong> {alert.harmonyWatcher.id}
                                    </PersonDetail>
                                    <PersonDetail>
                                        <strong>Age:</strong> {alert.person.age}
                                    </PersonDetail>
                                    <PersonDetail>
                                        <strong>Gender:</strong> {alert.person.gender}
                                    </PersonDetail>
                                    <PersonDetail>
                                        <strong>Harmony Score:</strong> {alert.person.harmonyScore}
                                    </PersonDetail>
                                    <PersonDetail>
                                        <strong>Time:</strong> {formatTime(alert.time)}
                                    </PersonDetail>
                                    <PersonDetail>
                                        <strong>Location:</strong> {alert.harmonyWatcher.location.latitude},
                                        {alert.harmonyWatcher.location.longitude}
                                    </PersonDetail>
                                </>
                            ) : (
                                <PersonDetail>Person details not available</PersonDetail>
                            )}
                        </PersonInfoContainer>
                        <ProgressContainer>
                            <CircularProgressbar
                                value={alert.person ? alert.person.harmonyScore : 0}
                                text={`${alert.person ? alert.person.harmonyScore : 0}`}
                                styles={buildStyles({
                                    textSize: '20px',
                                    pathColor: getProgressColor(alert.person ? alert.person.harmonyScore : 0),
                                    textColor: '#333',
                                    trailColor: '#f2f2f2',
                                    backgroundColor: '#3e98c7',
                                    strokeLinecap: 'butt',
                                })}
                            />
                        </ProgressContainer>
                    </AlertCard>
                ))}
            </AlertListContainer>
        </AppContainer>
    );
};

export default AlertInterface;
