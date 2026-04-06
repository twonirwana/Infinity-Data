# Infinity Data

A program to convert unit data from Corvus Belli's Army Builder for the Infinity tabletop game.

## Unit Cards
Currently, the main function is to take an army code and generate nice, printable cards for all the units in the army.

The default implementation creates the cards in the A4 ratio, so 9 cards can be printing on a single A4 page.

The cards for the army code:

``hE0DanNhASCBLAIBAQAIAICQAQMAAICQAQQAAICTAQUAAIcZAQEAAIb%2FAQEAAIcZAQYAAICeAQIAAICeAQEAAgEABwCHHQEBAACHHAEDAACD6gEBAACApAECAACHIQECAACA3wEBAACAmAECAA%3D%3D``

look like this:

![img.png](img.png)

or as pdf: [JSA-inch.pdf](JSA-inch.pdf)

## Overview

It is also possible to use the army code to print an alternative list overview one or two pages, like these:

<img src="overview-bw.png" alt="bw" width="300"/>

or

<img src="overview-color.png" alt="color" width="300"/>


## Usage

The card generator is available under https://infinity.2nirwana.de/cards/ , simply insert your army code, select your
options (card style, inch/cm, all/distinct, ammo&ps/saving roll) and generate the cards. A new browser tap opens with
the cards. Simply
print them from your browser to pdf or on paper by selecting 9 on one-page option of the print dialoge.

## Development

Contributions of any kind are welcome, especially if someone has experience in HTML layout and wants to help make the
cards look nicer.

Based on [Infinity-Army-Tools](https://github.com/cwoac/Infinity-Army-Tools)